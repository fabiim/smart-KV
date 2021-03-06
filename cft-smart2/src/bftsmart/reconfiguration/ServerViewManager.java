/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.reconfiguration;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import bftsmart.reconfiguration.views.View;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.TOMUtil;

/**
 *
 * @author eduardo
 */
public class ServerViewManager extends ViewManager {

    public static final int ADD_SERVER = 0;
    public static final int REMOVE_SERVER = 1;
    public static final int CHANGE_F = 2;
    
    private int quorumF; // f replicas
    private int quorumCFT_Strong; // QUORUM PARA CASO CFT // QUORUM UTILIZADO PARA SABER O MINIMO DE RESPOSTAS NECESSARIAS PARA CHEGAR A UMA CONCLUSÃO
    private int quorum2F; // f * 2 replicas // QUORUM UTILIZADO PARA SABER O MINIMO DE RESPOSTAS NECESSARIAS PARA CHEGAR A UMA CONCLUSÃO
    private int quorumStrong; // ((n + f) / 2) replicas
    private int quorumFastDecide; // ((n + 3 * f) / 2) replicas
    private int[] otherProcesses;
    private int[] lastJoinStet;
    private List<TOMMessage> updates = new LinkedList<TOMMessage>();
    private TOMLayer tomLayer;
   // protected View initialView;
    
    public ServerViewManager(int procId) {
    	this(procId,"");
        /*super(procId);
        initialView = new View(0, getStaticConf().getInitialView(), 
                getStaticConf().getF(), getInitAdddresses());
        getViewStore().storeView(initialView);
        reconfigureTo(initialView);*/
    }

    public ServerViewManager(int procId, String configHome) {
        super(procId, configHome);
        View cv = getViewStore().readView();
        if(cv == null){
            reconfigureTo(new View(0, getStaticConf().getInitialView(), 
                getStaticConf().getF(), getInitAdddresses()));
        }else{
            reconfigureTo(cv);
        }
    }

    private InetSocketAddress[] getInitAdddresses() {

        int nextV[] = getStaticConf().getInitialView();
        InetSocketAddress[] addresses = new InetSocketAddress[nextV.length];
        for (int i = 0; i < nextV.length; i++) {
            addresses[i] = getStaticConf().getRemoteAddress(nextV[i]);
        }// QUORUM UTILIZADO PARA SABER O MINIMO DE RESPOSTAS NECESSARIAS PARA CHEGAR A UMA CONCLUSÃO

        return addresses;
    }
    
    public void setTomLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }

    
    public boolean isInCurrentView() {
        return this.currentView.isMember(getStaticConf().getProcessId());
    }

    public int[] getCurrentViewOtherAcceptors() {
        return this.otherProcesses;
    }

    public int[] getCurrentViewAcceptors() {
        return this.currentView.getProcesses();
    }

    public boolean hasUpdates() {
        return !this.updates.isEmpty();
    }

    public void enqueueUpdate(TOMMessage up) {
        ReconfigureRequest request = (ReconfigureRequest) TOMUtil.getObject(up.getContent());
        if (TOMUtil.verifySignature(getStaticConf().getRSAPublicKey(request.getSender()),
                request.toString().getBytes(), request.getSignature())) {
            if (request.getSender() == getStaticConf().getTTPId()) {
                this.updates.add(up);
            } else {
                boolean add = true;
                Iterator<Integer> it = request.getProperties().keySet().iterator();
                while (it.hasNext()) {
                    int key = it.next();
                    String value = request.getProperties().get(key);
                    if (key == ADD_SERVER) {
                        StringTokenizer str = new StringTokenizer(value, ":");
                        if (str.countTokens() > 2) {
                            int id = Integer.parseInt(str.nextToken());
                            if(id != request.getSender()){
                                add = false;
                            }
                        }else{
                            add = false;
                        }
                    } else if (key == REMOVE_SERVER) {
                        if (isCurrentViewMember(Integer.parseInt(value))) {
                            if(Integer.parseInt(value) != request.getSender()){
                                add = false;
                            }
                        }else{
                            add = false;
                        }
                    } else if (key == CHANGE_F) {
                        add = false;
                    }
                }
                if(add){
                    this.updates.add(up);
                }
            }
        }
    }

    public byte[] executeUpdates(int eid, int decisionRound) {


        List<Integer> jSet = new LinkedList<Integer>();
        List<Integer> rSet = new LinkedList<Integer>();
        int f = -1;
        
        List<String> jSetInfo = new LinkedList<String>();
        
        
        for (int i = 0; i < updates.size(); i++) {
            ReconfigureRequest request = (ReconfigureRequest) TOMUtil.getObject(updates.get(i).getContent());
            Iterator<Integer> it = request.getProperties().keySet().iterator();

            while (it.hasNext()) {
                int key = it.next();
                String value = request.getProperties().get(key);

                if (key == ADD_SERVER) {
                    StringTokenizer str = new StringTokenizer(value, ":");
                    if (str.countTokens() > 2) {
                        int id = Integer.parseInt(str.nextToken());
                        if(!isCurrentViewMember(id) && !contains(id, jSet)){
                            jSetInfo.add(value);
                            jSet.add(id);
                            String host = str.nextToken();
                            int port = Integer.valueOf(str.nextToken());
                            this.getStaticConf().addHostInfo(id, host, port);
                        }
                    }
                } else if (key == REMOVE_SERVER) {
                    if (isCurrentViewMember(Integer.parseInt(value))) {
                        rSet.add(Integer.parseInt(value));
                    }
                } else if (key == CHANGE_F) {
                    f = Integer.parseInt(value);
                }
            }

        }
        //ret = reconfigure(updates.get(i).getContent());
        return reconfigure(jSetInfo, jSet, rSet, f, eid, decisionRound);
    }

    private boolean contains(int id, List<Integer> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).intValue() == id) {
                return true;
            }
        }
        return false;
    }

    private byte[] reconfigure(List<String> jSetInfo, List<Integer> jSet, List<Integer> rSet, int f, int eid, int decisionRound) {
        //ReconfigureRequest request = (ReconfigureRequest) TOMUtil.getObject(req);
        // Hashtable<Integer, String> props = request.getProperties();
        // int f = Integer.valueOf(props.get(CHANGE_F));
        lastJoinStet = new int[jSet.size()];
        int[] nextV = new int[currentView.getN() + jSet.size() - rSet.size()];
        int p = 0;
        
        boolean forceLC = false;
        for (int i = 0; i < jSet.size(); i++) {
            lastJoinStet[i] = jSet.get(i);
            nextV[p++] = jSet.get(i);
        }

        for (int i = 0; i < currentView.getProcesses().length; i++) {
            if (!contains(currentView.getProcesses()[i], rSet)) {
                nextV[p++] = currentView.getProcesses()[i];
            } else if (tomLayer.lm.getCurrentLeader() == currentView.getProcesses()[i]) {
                
                forceLC = true;
 
            }
        }

        if (f < 0) {
            f = currentView.getF();
        }

        InetSocketAddress[] addresses = new InetSocketAddress[nextV.length];

        for(int i = 0 ;i < nextV.length ;i++)
        	addresses[i] = getStaticConf().getRemoteAddress(nextV[i]);

        View newV = new View(currentView.getId() + 1, nextV, f,addresses);

        System.out.println("new view: " + newV);
        System.out.println("installed on eid: " + eid);
        System.out.println("lastJoinSet: " + jSet);

        //TODO:Remove all information stored about each process in rSet
        //processes execute the leave!!!
        reconfigureTo(newV);
        
        if (forceLC) {
            
            //TODO: Reactive it and make it work
            System.out.println("Shortening LC timeout");
            tomLayer.requestsTimer.stopTimer();
            tomLayer.requestsTimer.setShortTimeout(3000);
            tomLayer.requestsTimer.startTimer();
            //tomLayer.triggerTimeout(new LinkedList<TOMMessage>());
                
        } 
        return TOMUtil.getBytes(new ReconfigureReply(newV, jSetInfo.toArray(new String[0]),
                 eid, tomLayer.lm.getCurrentLeader() /*tomLayer.lm.getLeader(eid, decisionRound)*/));
    }

    public TOMMessage[] clearUpdates() {
        TOMMessage[] ret = new TOMMessage[updates.size()];
        for (int i = 0; i < updates.size(); i++) {
            ret[i] = updates.get(i);
        }
        updates.clear();
        return ret;
    }

    public boolean isInLastJoinSet(int id) {
        if (lastJoinStet != null) {
            for (int i = 0; i < lastJoinStet.length; i++) {
                if (lastJoinStet[i] == id) {
                    return true;
                }
            }

        }
        return false;
    }

    public void processJoinResult(ReconfigureReply r) {
        this.reconfigureTo(r.getView());
        
        String[] s = r.getJoinSet();
        
        this.lastJoinStet = new int[s.length];
        
        for(int i = 0; i < s.length;i++){
             StringTokenizer str = new StringTokenizer(s[i], ":");
             int id = Integer.parseInt(str.nextToken());
             this.lastJoinStet[i] = id;
             String host = str.nextToken();
             int port = Integer.valueOf(str.nextToken());
             this.getStaticConf().addHostInfo(id, host, port);
        }
    }

    
    @Override
    public final void reconfigureTo(View newView) {
        this.currentView = newView;
        getViewStore().storeView(this.currentView);
        if (newView.isMember(getStaticConf().getProcessId())) {
            //É membro da view atual
            otherProcesses = new int[currentView.getProcesses().length - 1];
            int c = 0;
            for (int i = 0; i < currentView.getProcesses().length; i++) {
                if (currentView.getProcesses()[i] != getStaticConf().getProcessId()) {
                    otherProcesses[c++] = currentView.getProcesses()[i];
                }
            }

            this.quorumF = this.currentView.getF();
            //this.quorumCFT_F1 = this.quorumF; //isto é assim porque do outro lado é maior--> logo maior que f = f+1
            this.quorum2F = 2 * this.quorumF;
            this.quorumStrong = (int) Math.ceil((this.currentView.getN() + this.quorumF) / 2);
            this.quorumCFT_Strong = (int) Math.ceil(this.currentView.getN() / 2);
            this.quorumFastDecide = (int) Math.ceil((this.currentView.getN() + 3 * this.quorumF) / 2);
        } else if (this.currentView != null && this.currentView.isMember(getStaticConf().getProcessId())) {
            //TODO: Left the system in newView -> LEAVE
            //CODE for LEAVE   
        }else{
            //TODO: Didn't enter the system yet
            
        }
    }

    /**
     * This is the certificate quorum ncessary for some parts of the protocol.
     * Byzantine case = 2f+1
     * CFT case = f+1
     * @return
     */
    public int getCertificateQuorum() {
    	//return quorum2F;
    	return getStaticConf().isBFT() ? quorum2F : quorumF;
    }

    public int getQuorumF() {
        return quorumF;
    }
/*
    public int getQuorumFastDecide() {
        return quorumFastDecide;
    }*/

    public int getQuorumStrong() {
    	return getStaticConf().isBFT() ? quorumStrong : quorumCFT_Strong;
        //return quorumStrong;
    }
}
