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

package org.redhat.gitparser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * @author Clebert Suconic
 */

public class ArtemisParser {

   public static void main(String arg[]) {
      try {
         if (arg.length != 5) {
            System.err.println("use Parser <repository> <reportOutput> <from> <to> <rest : true|false>");
            System.exit(-1);
         }

         boolean rest = Boolean.parseBoolean(arg[4]);

         GitParser parser = new GitParser(new File(arg[0]), "ARTEMIS-", "https://issues.apache.org/jira/browse/", "https://github.com/apache/activemq-artemis/").
            setSourceSuffix(".java", ".md", ".c", ".sh", ".groovy").
            setSampleJQL("https://issues.apache.org/jira/issues/?jql=project%20%3D%20ARTEMIS%20AND%20key%20in%20");

         if (rest) {
            parser.setRestLocation("https://issues.apache.org/jira/rest/api/2/issue/");
         }

         parser.addInterestingfolder("test").addInterestingfolder("docs/").addInterestingfolder("examples/");
         File file = new File(arg[1]);
         parser.parse(file, arg[2], arg[3]);

      } catch (Exception e) {
         e.printStackTrace();
         System.exit(-1);
      }
   }
}
