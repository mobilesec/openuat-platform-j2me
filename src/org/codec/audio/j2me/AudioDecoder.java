/**
 * Modified by Iulia Ion
 */

package org.codec.audio.j2me;

/**
 * Copyright 2002 by the authors. All rights reserved.
 *
 * Author: Cristina V Lopes
 */


import org.codec.utils.ArrayUtils;
import org.codec.utils.Constants;

import java.io.ByteArrayOutputStream;

/**
 * Copyright (c) 2007, Regents of the University of California
 * All rights reserved.
 * ====================================================================
 * Licensed under the BSD License. Text as follows.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   - Neither the name of University of California,Irvine nor the names
 *     of its contributors may be used to endorse or promote products 
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * @author Crista Lopes (lopes at uci dot edu)
 * @version 1.0
 * 
 */
public class AudioDecoder implements Constants {

    /**
     * @param signal          the audio samples to search
     * @param signalStrengths this will be filled in with the strengths for each frequency (NOTE THIS SIDE EFFECT)
     * @param granularity     a correlation will be determined every granularity samples (lower is slower)
     * @return the index in signal of the key sequence, or -1 if it wasn't found (in which case signalStrengths is trashed)
     */
    public static int findKeySequence(byte[] signal, double[] signalStrengths, int granularity) {
        int maxCorrelationIndex = -1;
        double maxCorrelation = -1;
        double minSignal = 0.003;
        double acceptedSignal = 0.01;
        int i = 0;
        for (i = 0; i <= signal.length - kSamplesPerDuration; i += granularity) {
            //test the correlation
            byte[] partialSignal = ArrayUtils.subarray(signal, i, kSamplesPerDuration);
            double corr = complexDetect(partialSignal, Constants.kHailFrequency) /* * 4 */;
            //	    System.out.println("Correlation at " + i + ":" + corr);
            if (corr > maxCorrelation) {
                maxCorrelation = corr;
                maxCorrelationIndex = i;
            }
            if (granularity <= 0) {
                break;
            }
        }

        //System.out.println("Searched to index:" + i);
        if (maxCorrelation < acceptedSignal && maxCorrelation > -1) {
            //System.out.println("Best Correlation:" + maxCorrelation);
            maxCorrelationIndex = -1;
        }
        //if(maxCorrelationIndex >= 0){
        //System.out.println("\r\nMax Correlation:" + maxCorrelation + " index:" + maxCorrelationIndex);
        //System.out.println("signal.length:" + signal.length);
        //getKeySignalStrengths(org.codec.utils.ArrayUtils.subarray(signal, maxCorrelationIndex + kSamplesPerDuration,
        //					      kSamplesPerDuration * 2),
        //		  signalStrengths);
        //}

        return maxCorrelationIndex;
    }

    /**
     * @param startSignals the signal strengths of each of the frequencies
     * @param samples      the samples
     * @return the decoded bytes
     */
    public static byte[] decode(double[] startSignals, byte[] samples) {
        return decode(startSignals, getSignalStrengths(samples));
    }

    /**
     * @param startSignals the signal strengths of each of the frequencies
     * @param signal       the signal strengths for each frequency for each duration [strength][duration index]
     *                     SIDE EFFECT: THE signal PARAMETER WILL BE SCALED BY THE STARTSIGNALS
     * @return the decoded bytes
     */
    private static byte[] decode(double[] startSignals, double[][] signal) {
        //normalize to the start signals
        for (int i = 0; i < (kBitsPerByte * kBytesPerDuration); i++) {
            for (int j = 0; j < signal[i].length; j++) {
                signal[i][j] = signal[i][j] / startSignals[i];
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < signal[0].length; i++) {
            for (int k = 0; k < kBytesPerDuration; k++) {
                byte value = 0;
                for (int j = 0; j < kBitsPerByte; j++) {
                    if (signal[(k * kBitsPerByte) + j][i] > 0.4) {
                        value = (byte) (value | (1 << j));
                    } else {
                    }
                }
                baos.write(value);
            }
        }

        return baos.toByteArray();
    }

    /**
     * @param input audio sample array
     * @return the signal strengths of each frequency in each duration: [signal strength][duration index]
     */
    private static double[][] getSignalStrengths(byte[] input) {
        //detect the signal strength of each frequency in each duration
        int durations = input.length / kSamplesPerDuration;

        // rows are durations, cols are bit strengths
        double[][] signal = new double[kBitsPerByte * kBytesPerDuration][durations];

        //for each duration, check each bit for representation in the input
        for (int i = 0; i < durations; i++) {
            //separate this duration's input into its own array
            byte[] durationInput = ArrayUtils.subarray(input, i * kSamplesPerDuration, kSamplesPerDuration);

            //for each bit represented, detect
            for (int j = 0; j < kBitsPerByte * kBytesPerDuration; j++) {
                signal[j][i] =
                        complexDetect(durationInput, AudioEncoder.getFrequency(j));
                /*
            if (j == 0)
              System.out.println("\nsignal[" + j + "][" + i + "]=" + signal [j][i]);
            else
              System.out.println("signal[" + j + "][" + i + "]=" + signal [j][i]);
            */
            }
        }
        return signal;
    }

    public static void getKeySignalStrengths(byte[] signal, double[] signalStrengths) {
        byte[] partialSignal = ArrayUtils.subarray(signal, 0, kSamplesPerDuration);
        for (int j = 1; j < kBitsPerByte * kBytesPerDuration; j += 2) {
            signalStrengths[j] = complexDetect(partialSignal, AudioEncoder.getFrequency(j));
        }

        byte[] partialSignal2 = ArrayUtils.subarray(signal, kSamplesPerDuration, kSamplesPerDuration);
        for (int j = 0; j < kBitsPerByte * kBytesPerDuration; j += 2) {
            signalStrengths[j] = complexDetect(partialSignal2, AudioEncoder.getFrequency(j));
            //System.out.println(signalStrengths[j]);
        }
    }

    /**
     * @param signal    audio samples
     * @param frequence the frequency to search for in signal
     * @return the strength of the correlation of the frequency in the signal
     */
    private static double complexDetect(byte[] signal, double frequency) {
        double realSum = 0;
        double imaginarySum = 0;
        double u = 2 * Math.PI * frequency / kSamplingFrequency;
        // y = e^(ju) = cos(u) + j * sin(u)

        for (int i = 0; i < signal.length; i++) {
            //System.out.println("signal[" +i +"]: " +signal[i] + "; convert: " + (signal[i])/(float)org.codec.utils.Constants.kFloatToByteShift);
            realSum = realSum + (Math.cos(i * u) * (signal[i] / (float) Constants.kFloatToByteShift));
            imaginarySum = imaginarySum + (Math.sin(i * u) * (signal[i] / (float) Constants.kFloatToByteShift));
        }
        //System.out.println("realSum=" + realSum + "; imSum=" + imaginarySum);
        double realAve = realSum / signal.length;
        double imaginaryAve = imaginarySum / signal.length;
//   	System.out.println("u:" + u + " realAve:" + realAve + " imaginaryAve:" + imaginaryAve 
//   			   + " \r\nfrequency:" + frequency + " signal.length:" + signal.length
//   			   + " realSum:" + realSum + " imaginarySum:" + imaginarySum 
//   			   + "signal[100]:" + (signal[100]/(float)org.codec.utils.Constants.kFloatToByteShift));
        // return the abs ( realAve + imaginaryAve * i ) which equals sqrt( realAve^2 + imaginaryAve^2)
        return Math.sqrt((realAve * realAve) + (imaginaryAve * imaginaryAve));
    }
}
