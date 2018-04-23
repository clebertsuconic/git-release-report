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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * @author Clebert Suconic
 */

public class Parser {

   public static void main(String arg[]) {
      try {
         if (arg.length < 3) {
            System.err.println("use Parser <repository> <from> <to>");
            System.exit(-1);
         }
         Git git = Git.open(new File(arg[0]));

         RevWalk walk = new RevWalk(git.getRepository());
         LogCommand log = git.log();

         ObjectId from = git.getRepository().resolve(arg[1]);
         ObjectId to = git.getRepository().resolve(arg[2]);

         RevCommit fromCommit = walk.parseCommit(from);
         RevCommit toCommit = walk.parseCommit(to);

         System.out.println("from::" + fromCommit);
         System.out.println("to::" + toCommit);

         log.setRevFilter(RevFilter.NO_MERGES);
         log.addRange(fromCommit, toCommit);

         for (RevCommit obj : log.call()) {
            System.out.println("-----------");
            System.out.println(obj.getFullMessage());

            TreeWalk treeWalk = new TreeWalk(git.getRepository());
            treeWalk.reset(obj.getId());
            while (treeWalk.next()) {
               String path = treeWalk.getPathString();
               System.out.println("path changed " + path);
            }
            treeWalk.close();
         }
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(-1);
      }
   }

   private static AbstractTreeIterator prepareTree(Git git, RevWalk walk, RevCommit commit) throws Exception {
      RevTree tree = walk.parseTree(commit.getTree().getId());

      CanonicalTreeParser treeParser = new CanonicalTreeParser();
      try (ObjectReader reader = git.getRepository().newObjectReader()) {
         treeParser.reset(reader, tree.getId());
      }

      return treeParser;
   }
}
