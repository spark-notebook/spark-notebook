/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook.kernel.pfork;

public final class ChildProcessMain {
    public static void main(String[] args) {
        BetterFork$.MODULE$.main(args);
    }
}