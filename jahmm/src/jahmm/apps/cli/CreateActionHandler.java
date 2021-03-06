/*
 * Copyright (c) 2004-2009, Jean-Marc François. All Rights Reserved.
 * Licensed under the New BSD license.  See the LICENSE file.
 */
package jahmm.apps.cli;

import jahmm.RegularHmmBase;
import jahmm.apps.cli.CommandLineArguments.Arguments;
import jahmm.io.HmmWriter;
import jahmm.io.OpdfWriter;
import jahmm.observables.CentroidFactory;
import jahmm.observables.Observation;
import jahmm.observables.Opdf;
import jahmm.observables.OpdfFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.EnumSet;

/**
 * Creates a Hmm and write it to file.
 */
class CreateActionHandler
        extends ActionHandler {

    @Override
    public void act()
            throws FileNotFoundException, IOException, AbnormalTerminationException {
        EnumSet<Arguments> args = EnumSet.of(
                Arguments.OPDF,
                Arguments.NB_STATES,
                Arguments.OUT_HMM);
        CommandLineArguments.checkArgs(args);

        int nbStates = Arguments.NB_STATES.getAsInt();
        OutputStream outStream = Arguments.OUT_HMM.getAsOutputStream();
        Writer outWriter = new OutputStreamWriter(outStream);

        write(outWriter, nbStates, Types.relatedObjs());

        outWriter.flush();
    }

    private <O extends Observation & CentroidFactory<O>> void
            write(Writer outWriter, int nbStates, RelatedObjs<O> relatedObjs)
            throws IOException {
        OpdfFactory<? extends Opdf<O>> opdfFactory = relatedObjs.opdfFactory();
        OpdfWriter<? extends Opdf<O>> opdfWriter = relatedObjs.opdfWriter();

        RegularHmmBase<O> hmm = new RegularHmmBase<>(nbStates, opdfFactory);

        HmmWriter.write(outWriter, opdfWriter, hmm);
    }
}
