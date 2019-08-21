package org.jlab.rec.tof.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlab.rec.tof.hit.AHit;

/**
 *
 * @author ziegler
 *
 */
public class ClusterFinder {

    public ClusterFinder() {
        // TODO Auto-generated constructor stub
    }
    
    Map<Integer, AHit[][][]> HitArrays;
    public void fillHitArrays(List<?> hits2, int nsectors,
            int npanels, int[] npaddles) {
        HitArrays = new HashMap<Integer, AHit[][][]>() ;
        
        if (hits2 != null) {
            int maxNumPad = 0;
            for (int i = 0; i < npaddles.length; i++) {
                if (npaddles[i] > maxNumPad) {
                    maxNumPad = npaddles[i];
                }
            }
            // a Hit Array is used to identify clusters, panel1B is the one with
            // the maximum number of scintillator bars
            
            double minEn=(hits2.get(0) instanceof org.jlab.rec.tof.hit.ftof.Hit)?  
                    org.jlab.rec.ftof.Constants.MINENERGY:org.jlab.rec.ctof.Constants.MINENERGY;
            // initializing non-zero Hit Array entries
            // with valid hits
            for (int i = 0; i < hits2.size(); i++) {
                //energy cut
                if (((AHit) hits2.get(i)).get_Energy() < minEn) {
                    continue;
                }
                //match to track
                if (((AHit) hits2.get(i)).get_TrkPosition() == null)
                    continue;

                int id = ((AHit) hits2.get(i))._AssociatedTrkId;
                //create arrays for hits matched to tracks
                HitArrays.put(id, new AHit[maxNumPad][npanels][nsectors]);
            }
            for (int i = 0; i < hits2.size(); i++) {
                //energy cut
                if (((AHit) hits2.get(i)).get_Energy() < minEn) {
                    continue;
                }
                //match to track
                if (((AHit) hits2.get(i)).get_TrkPosition() == null)
                    continue;
            
                int id = ((AHit) hits2.get(i))._AssociatedTrkId;
            
                int w = ((AHit) hits2.get(i)).get_Paddle();
                int l = ((AHit) hits2.get(i)).get_Panel();
                int s = ((AHit) hits2.get(i)).get_Sector();

                if (s > 0 && s <= nsectors && l > 0 && l <= npanels && w > 0
                        && w <= npaddles[l - 1]) {
                    AHit[][][] thisHitArray = HitArrays.get(id);
                    thisHitArray[w - 1][l - 1][s - 1] = (AHit) hits2.get(i);
                }

            }
        }
    }

    /**
     * int panel, int sector, int paddle 
     */
    public ArrayList<Cluster> findClusters(List<?> hits2, int nsectors,
            int npanels, int[] npaddles) {
        ArrayList<Cluster> clusters = new ArrayList<Cluster>();
        if (hits2 != null) {
            
            this.fillHitArrays(hits2, nsectors, npanels, npaddles);
            
            int cid = 1; // cluster id, will increment with each new good
            // cluster
            
            for (Integer tid : HitArrays.keySet()) {               
                AHit[][][] HitArray  = HitArrays.get(tid);
                // for each panel and sector, a loop over the components
                for (int s = 0; s < nsectors; s++) {
                    for (int l = 0; l < npanels; l++) {
                        int si = 0; // index in the loop
                        // looping over all bars
                        while (si < npaddles[l]) {
                            // if there's a hit, it's a cluster candidate
                            if (HitArray[si][l][s] != null) {
                                // array of hits in the cluster candidate
                                ArrayList<AHit> hits = new ArrayList<AHit>();
                                try {
                                    while (HitArray[si][l][s] != null
                                            && si < npaddles[l]) {
                                        AHit clusteredHit = HitArray[si][l][s];
                                        hits.add(clusteredHit);
                                        si++;
                                    }
                                } catch (ArrayIndexOutOfBoundsException exception) {
                                    continue;
                                }
                                // define new cluster
                                Cluster this_cluster = new Cluster(s + 1, l + 1,
                                        cid++);
                                // add hits to the cluster
                                hits = this.HitsToCluster(hits);
                                this_cluster.addAll(hits);
                                // make arraylist
                                for (AHit hit : this_cluster) {
                                    hit.set_AssociatedClusterID(this_cluster
                                            .get_Id());
                                }
                                this_cluster.calc_Centroids();
                                this_cluster.matchToTrack();
                                clusters.add(this_cluster);
                            }
                            // if no hits, check for next
                            si++;
                        }
                    }
                }
            }
        }
        return clusters;
        
    }

    private ArrayList<AHit> HitsToCluster(List<?> hits) {
        //Sort hits by energy
        ArrayList<AHit> hits2 = (ArrayList<AHit>) hits;
        hits2.sort(Comparator.comparing(AHit::get_Paddle).thenComparing(AHit::get_Energy));
        // Energy Selection goes here:
        return (ArrayList<AHit>) hits2;
    }

    
}
