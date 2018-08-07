package com.gitblit.plugin.autobranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import ro.fortsoft.pf4j.Extension;

import com.gitblit.extensions.RepositoryLifeCycleListener;
import com.gitblit.models.RepositoryModel;
import com.gitblit.servlet.GitblitContext;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;


/**
 * AutoBranch Hook for Saritasa gitblit
 *
 * @author RomanCherednikov
 *
 */

@Extension
public class AutoBranchHook extends RepositoryLifeCycleListener {

    @Override
    public void onCreation(RepositoryModel repo) {

        // init gitblit managers
        final Logger log = LoggerFactory.getLogger(getClass());
        final IRepositoryManager repositoryManager = GitblitContext.getManager(IRepositoryManager.class);
        final IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
        final Repository repository = repositoryManager.getRepository(repo.name);

        // commit message and author
        PersonIdent author = new PersonIdent("gitblit", "gitblit@localhost");
        String message = "default branch";

        String branches = runtimeManager.getSettings().getString(Plugin.AUTO_BRANCH_LIST, "develop, master");
        List<String> branchList = Arrays.asList(branches.split("\\s*,\\s*"));

        log.info("[AutoBranch plugin] Creating branches: {} in {} repository", branchList, repo);

        try {
            ObjectInserter objectDataInsert = repository.newObjectInserter();

            try {
                ObjectId blobId = objectDataInsert.insert(Constants.OBJ_BLOB, message.getBytes(Constants.CHARACTER_ENCODING));

                for (String branch : branchList) {

                    // set correct ref
                    branch = "refs/heads/" + branch;

                    // create tree
                    TreeFormatter tree = new TreeFormatter();
                    tree.append(".branch", FileMode.REGULAR_FILE, blobId);
                    ObjectId treeId = objectDataInsert.insert(tree);

                    // Create a commit object
                    CommitBuilder commit = new CommitBuilder();
                    commit.setAuthor(author);
                    commit.setCommitter(author);
                    commit.setEncoding(Constants.CHARACTER_ENCODING);
                    commit.setMessage(message);
                    commit.setTreeId(treeId);

                    // insert commit
                    ObjectId commitId = objectDataInsert.insert(commit);
                    objectDataInsert.flush();

                    // ref update
                    RevWalk revWalk = new RevWalk(repository);
                    try {
                        RevCommit revCommit = revWalk.parseCommit(commitId);
                        RefUpdate ru = repository.updateRef(branch);
                        ru.setNewObjectId(commitId);
                        ru.setRefLogMessage("commit: " + revCommit.getShortMessage(), false);
                        Result rc = ru.forceUpdate();
                    } finally {
                        revWalk.close();
                    }
                }

            } finally {
                objectDataInsert.close();
            }

        } catch (Throwable t) {
            log.info("[AutoBranch plugin] Failed to create branches: {} in {} repository", branchList, repo);
        }

        // set default headRef
        String defaultHead = "refs/heads/" + branchList.get(0);

        try {
            RefUpdate head = repository.updateRef(Constants.HEAD, false);
            RefUpdate.Result result;
            result = head.link(defaultHead);
        } catch  (Throwable t) {
            log.info("[AutoBranch plugin] Failed to set default head: {} in {} repository", defaultHead, repo);
        }

    }

    @Override
    public void onFork(RepositoryModel origin, RepositoryModel fork) {}

    @Override
    public void onRename(String oldName, RepositoryModel repo) {}

    @Override
    public void onDeletion(RepositoryModel repo) {}
}