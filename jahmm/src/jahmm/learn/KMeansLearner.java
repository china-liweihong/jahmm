/*
 * Copyright (c) 2004-2009, Jean-Marc François. All Rights Reserved.
 * Licensed under the New BSD license.  See the LICENSE file.
 */
package jahmm.learn;

import jahmm.RegularHmmBase;
import jahmm.calculators.KMeansCalculator;
import jahmm.calculators.RegularViterbiCalculatorBase;
import jahmm.observables.CentroidFactory;
import jahmm.observables.Observation;
import jahmm.observables.Opdf;
import jahmm.observables.OpdfFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

/**
 * An implementation of the K-Means learning algorithm.
 *
 * @param <O>
 */
public class KMeansLearner<O extends Observation & CentroidFactory<? super O>> {

    static <T> List<T> flat(List<? extends List<? extends T>> lists) {
        List<T> v = new ArrayList<>();

        for (List<? extends T> list : lists) {
            v.addAll(list);
        }

        return v;
    }

    private final Clusters<O> clusters;
    private final int nbStates;
    private final List<? extends List<? extends O>> obsSeqs;
    private final OpdfFactory<? extends Opdf<O>> opdfFactory;
    private boolean terminated;

    /**
     * Initializes a K-Means algorithm implementation. This algorithm finds a
     * HMM that models a set of observation sequences.
     *
     * @param nbStates The number of states the resulting HMM will be made of.
     * @param opdfFactory A class that builds the observation probability
     * distributions associated to the states of the HMM.
     * @param sequences A vector of observation sequences. Each observation
     * sequences is a vector of null null null null null null null null null
     * null null null null null null null null null null null null null null
     * null null null null null null null null null null null null null null
     * null null null null null null null null     {@link be.ac.ulg.montefiore.run.jahmm.Observation
	 *                observations} compatible with the null null null null null null null null
     * null null null null null null null null null null null null null null
     * null null null null null null null null null null null null null null
     * null null null null null null null null null     {@link be.ac.ulg.montefiore.run.jahmm.CentroidFactory
	 *                k-means algorithm}.
     * @throws java.lang.CloneNotSupportedException
     */
    public KMeansLearner(int nbStates,
            OpdfFactory<? extends Opdf<O>> opdfFactory,
            List<? extends List<? extends O>> sequences) throws CloneNotSupportedException {
        this.obsSeqs = sequences;
        this.opdfFactory = opdfFactory;
        this.nbStates = nbStates;

        List<? extends O> observations = flat(sequences);
        clusters = new Clusters<>(nbStates, observations);
        terminated = false;
    }

    /**
     * Performs one iteration of the K-Means algorithm. In one iteration, a new
     * HMM is computed using the current clusters, and the clusters are
     * re-estimated using this HMM.
     *
     * @return A new, updated HMM.
     */
    public RegularHmmBase<O> iterate() {
        RegularHmmBase<O> hmm = new RegularHmmBase<>(nbStates, opdfFactory);

        learnPi(hmm);
        learnAij(hmm);
        learnOpdf(hmm);

        terminated = optimizeCluster(hmm);

        return hmm;
    }

    /**
     * Returns <code>true</code> if the algorithm has reached a fix point, else
     * returns <code>false</code>.
     *
     * @return
     */
    public boolean isTerminated() {
        return terminated;
    }

    /**
     * Does iterations of the K-Means algorithm until a fix point is reached.
     *
     * @return The HMM that best matches the set of observation sequences given
     * (according to the K-Means algorithm).
     */
    public RegularHmmBase<O> learn() {
        RegularHmmBase<O> hmm;

        do {
            hmm = iterate();
        } while (!isTerminated());

        return hmm;
    }

    private void learnPi(RegularHmmBase<?> hmm) {
        double[] pi = new double[nbStates];

        for (int i = 0; i < nbStates; i++) {
            pi[i] = 0.;
        }

        for (List<? extends O> sequence : obsSeqs) {
            pi[clusters.clusterNb(sequence.get(0))]++;
        }

        for (int i = 0; i < nbStates; i++) {
            hmm.setPi(i, pi[i] / obsSeqs.size());
        }
    }

    private void learnAij(RegularHmmBase<O> hmm) {
        for (int i = 0; i < hmm.nbStates(); i++) {
            for (int j = 0; j < hmm.nbStates(); j++) {
                hmm.setAij(i, j, 0.);
            }
        }

        for (List<? extends O> obsSeq : obsSeqs) {
            if (obsSeq.size() < 2) {
                continue;
            }

            int first_state;
            int second_state = clusters.clusterNb(obsSeq.get(0));
            for (int i = 1; i < obsSeq.size(); i++) {
                first_state = second_state;
                second_state
                        = clusters.clusterNb(obsSeq.get(i));

                hmm.setAij(first_state, second_state,
                        hmm.getAij(first_state, second_state) + 1.);
            }
        }

        /* Normalize Aij array */
        for (int i = 0; i < hmm.nbStates(); i++) {
            double sum = 0;

            for (int j = 0; j < hmm.nbStates(); j++) {
                sum += hmm.getAij(i, j);
            }

            if (sum == 0.) {
                for (int j = 0; j < hmm.nbStates(); j++) {
                    hmm.setAij(i, j, 1. / hmm.nbStates());     // Arbitrarily
                }
            } else {
                for (int j = 0; j < hmm.nbStates(); j++) {
                    hmm.setAij(i, j, hmm.getAij(i, j) / sum);
                }
            }
        }
    }

    private void learnOpdf(RegularHmmBase<O> hmm) {
        for (int i = 0; i < hmm.nbStates(); i++) {
            Collection<O> clusterObservations = clusters.cluster(i);

            if (clusterObservations.isEmpty()) {
                hmm.setOpdf(i, opdfFactory.generate());
            } else {
                hmm.getOpdf(i).fit(clusterObservations);
            }
        }
    }

    /* Return true if no modification */
    private boolean optimizeCluster(RegularHmmBase<O> hmm) {
        boolean modif = false;

        for (List<? extends O> obsSeq : obsSeqs) {
            RegularViterbiCalculatorBase vc = new RegularViterbiCalculatorBase(obsSeq, hmm);
            int states[] = vc.stateSequence();

            for (int i = 0; i < states.length; i++) {
                O o = obsSeq.get(i);

                if (clusters.clusterNb(o) != states[i]) {
                    modif = true;
                    clusters.remove(o, clusters.clusterNb(o));
                    clusters.put(o, states[i]);
                }
            }
        }

        return !modif;
    }
}


/*
 * This class holds the matching between observations and clusters.
 */
class Clusters<O extends CentroidFactory<? super O>> {

    private final Hashtable<O, Value> clustersHash;
    private final ArrayList<Collection<O>> clusters;

    Clusters(int k, List<? extends O> observations) throws CloneNotSupportedException {

        clustersHash = new Hashtable<>();
        clusters = new ArrayList<>();

        KMeansCalculator<O> kmc = new KMeansCalculator<>(k, observations);

        for (int i = 0; i < k; i++) {
            Collection<O> cluster = kmc.cluster(i);
            clusters.add(cluster);

            for (O element : cluster) {
                clustersHash.put(element, new Value(i));
            }
        }
    }

    public boolean isInCluster(Observation o, int clusterNb) {
        return clusterNb(o) == clusterNb;
    }

    public int clusterNb(Observation o) {
        return clustersHash.get(o).getClusterNb();
    }

    public Collection<O> cluster(int clusterNb) {
        return clusters.get(clusterNb);
    }

    public void remove(Observation o, int clusterNb) {
        clustersHash.get(o).setClusterNb(-1);
        clusters.get(clusterNb).remove(o);
    }

    public void put(O o, int clusterNb) {
        clustersHash.get(o).setClusterNb(clusterNb);
        clusters.get(clusterNb).add(o);
    }

    class Value {

        private int clusterNb;

        Value(int clusterNb) {
            this.clusterNb = clusterNb;
        }

        void setClusterNb(int clusterNb) {
            this.clusterNb = clusterNb;
        }

        int getClusterNb() {
            return clusterNb;
        }
    }
}
