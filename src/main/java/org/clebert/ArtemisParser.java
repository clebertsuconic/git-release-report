/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.clebert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * @author Clebert Suconic
 */

public class ArtemisParser {

   public static void main(String arg[]) {
      try {
         if (arg.length < 3) {
            System.err.println("use Parser <repository> <from> <to>");
            System.exit(-1);
         }

         GitParser parser = new GitParser(new File(arg[0]), "ARTEMIS-", "https://issues.apache.org/jira/browse/", "https://github.com/apache/activemq-artemis/", ".java");
         parser.addInterestingfolder("tests/").addInterestingfolder("docs/");
         PrintStream stream = new PrintStream(new FileOutputStream("/tmp/report.html"));
         parser.parse(stream, arg[1], arg[2]);

      } catch (Exception e) {
         e.printStackTrace();
         System.exit(-1);
      }
   }
}
