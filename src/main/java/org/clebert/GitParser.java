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
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

/**
 * @author Clebert Suconic
 */

public class GitParser {

   final List<String> interestingFolder = new ArrayList<>();
   final File folder;
   final String jira;
   final String jiraBrowseURI;
   final String githubURI;
   final String sourceSuffix;

   final HashSet<String> totalJiras = new HashSet<>();

   public GitParser(File folder, String jira, String jiraBrowseURI, String githubURI, String sourceSuffix) {
      this.folder = folder;
      this.jira = jira;
      this.jiraBrowseURI = jiraBrowseURI;
      this.githubURI = githubURI;
      this.sourceSuffix = sourceSuffix;
   }

   public GitParser addInterestingfolder(String folder) {
      interestingFolder.add(folder);
      return this;
   }

   private String makeALink(String text, String uri) {
      return "<a href='" + uri + "'>" + text + "</a>";
   }

   private String commitCell(RevCommit commit) {
      String text = commit.getId().getName().substring(0, 6);

      return makeALink(text, githubURI + "commit/" + commit.getName());
   }

   public static String[] extractJIRAs(String jira, String message) {
      HashSet list = new HashSet(1);
      for (int jiraIndex = message.indexOf(jira); jiraIndex >= 0; jiraIndex = message.indexOf(jira, jiraIndex)) {
         StringBuffer jiraID = new StringBuffer(jira);

         for (int i = jiraIndex + jira.length(); i < message.length(); i++) {
            char charAt = message.charAt(i);
            if (charAt >= '0' && charAt <= '9') {
               jiraID.append(charAt);
            } else {
               break;
            }
         }
         list.add(jiraID.toString());
         jiraIndex++;
      }

      return (String[]) list.toArray(new String[list.size()]);
   }

   public String prettyCommitMessage(String message) {
      String[] jiras = extractJIRAs(jira, message);
      for (int i = 0; i < jiras.length; i++) {
         totalJiras.add(jiras[i]);
         message = message.replace(jiras[i], makeALink(jiras[i], jiraBrowseURI + jiras[i]));
      }

      return message;
   }

   public void parse(PrintStream output, String from, String to) throws Exception {
      Git git = Git.open(folder);
      RevWalk walk = new RevWalk(git.getRepository());

      ObjectId fromID = git.getRepository().resolve(from + "^1"); // ONE COMMIT BEFORE THE SELECTED AS WE NEED DIFFS
      ObjectId toID = git.getRepository().resolve(to);

      RevCommit fromCommit = walk.parseCommit(fromID);
      RevCommit toCommit = walk.parseCommit(toID);
      walk.markUninteresting(fromCommit);
      walk.markStart(toCommit);

      DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
      diffFormatter.setRepository(git.getRepository());

      walk.sort(RevSort.REVERSE, true);
      walk.setRevFilter(RevFilter.NO_MERGES);
      Iterator<RevCommit> commits = walk.iterator();

      ObjectReader reader = git.getRepository().newObjectReader();
      CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
      CanonicalTreeParser newTreeIter = new CanonicalTreeParser();

      RevCommit oldCommit = null;

      output.println("<html><body>");

      output.println("<table border=1 style=\"width:100%\">");

      DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

      StringBuffer interestingChanges[] = new StringBuffer[interestingFolder.size()];

      while (commits.hasNext()) {
         for (int i = 0; i < interestingFolder.size(); i++) {
            // the method to cleanup a stringbuffer is cpu intensive. sorry for the extra garbage
            // this piece of code is a piece of garbage anyways :) only intended for reporting!
            interestingChanges[i] = new StringBuffer();
         }

         RevCommit commit = commits.next();

         if (oldCommit != null) {
            output.print("<tr>");
            output.print("<td>" + commitCell(commit) + " </td>");
            output.print("<td>" + dateFormat.format(new Date(commit.getCommitTime())));
            output.print("<td>" + commit.getAuthorIdent().getName() + "</td>");
            output.print("<td>" + prettyCommitMessage(commit.getShortMessage()) + "</td>");

            oldTreeIter.reset(reader, oldCommit.getTree());
            newTreeIter.reset(reader, commit.getTree());

            List<DiffEntry> diffList = git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();

            int addition = 0, deletion = 0, replacement = 0;

            for (DiffEntry entry : diffList) {
               String path = entry.getNewPath();
               if (path.equals("/dev/null")) {
                  // this could happen on deleting a whole file
                  path = entry.getOldPath();
               }

               boolean interested = false;

               FileHeader header = diffFormatter.toFileHeader(entry);

               for (int i = 0; i < interestingFolder.size(); i++) {
                  if (path.startsWith(interestingFolder.get(i)) && path.endsWith(sourceSuffix)) {
                     interested = true;
                     File file = new File(path);
                     if (entry.getNewPath().equals("/dev/null")) {
                        interestingChanges[i].append(file.getName() + " "); // deleted, there's no link
                     } else {

                        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                        for (HunkHeader hunk : header.getHunks()) {
                           EditList edits = hunk.toEditList();
                           Iterator<Edit> editsIterator = edits.iterator();
                           while (editsIterator.hasNext()) {
                              Edit edit = editsIterator.next();
                              switch (edit.getType()) {
                                 case INSERT:
                                 case REPLACE:
                                    min = Math.min(min, edit.getBeginB());
                                    max = Math.max(max, edit.getEndB());
                                    break;
                                 case DELETE:
                                    min = Math.min(min, edit.getBeginA());
                                    max = Math.max(max, edit.getEndA());
                                    break;
                              }
                           }
                        }

                        interestingChanges[i].append(makeALink(file.getName(), githubURI + "blob/" + commit.getId().getName() + "/" + path + "#L" + (min + 1) + "-" + (max + 1)) + " ");
                     }
                  }
               }


               if (!interested && path.endsWith(sourceSuffix)) {
                  for (HunkHeader hunk : header.getHunks()) {
                     EditList edits = hunk.toEditList();
                     Iterator<Edit> editsIterator = edits.iterator();

                     if (!interested && path.endsWith(".java")) {

                        while (editsIterator.hasNext()) {
                           Edit edit = editsIterator.next();
                           switch (edit.getType()) {
                              case INSERT:
                                 addition += (edit.getEndB() - edit.getBeginB() + 1);
                                 break;
                              case DELETE:
                                 deletion += (edit.getEndA() - edit.getBeginA() + 1);
                                 break;
                              case REPLACE:
                                 replacement += (edit.getEndB() - edit.getBeginB() + 1);
                                 break;
                           }
                        }
                     }
                     //                  System.out.println("hunk::" + hunk);
                     //output.println("hunk:: " + hunk);
                     // System.out.println("At " + hunk.getNewStartLine(), hunk.ge)
                  }
               }
            }
            output.print("<td>" + addition + "</td><td>" + replacement + "</td><td>" + deletion + "</td>");

            for (int i = 0; i < interestingChanges.length; i++) {
               output.print("<td>" + interestingChanges[i].toString() + "</td>");
            }
            output.println("</tr>");
         }
         oldCommit = commit;

      }

      output.println("</body></html>");

   }

   public List<String> getInterestingFolder() {
      return interestingFolder;
   }

   public File getFolder() {
      return folder;
   }

   public String getJira() {
      return jira;
   }

   public String getJiraBrowseURI() {
      return jiraBrowseURI;
   }

   public String getGithubURI() {
      return githubURI;
   }

   public HashSet<String> getTotalJiras() {
      return totalJiras;
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
