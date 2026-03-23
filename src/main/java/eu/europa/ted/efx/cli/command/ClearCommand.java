/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.cli.command;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "clear", description = "Clear the terminal screen")
public class ClearCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        return 0;
    }
}
