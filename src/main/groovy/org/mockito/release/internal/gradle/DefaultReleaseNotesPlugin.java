package org.mockito.release.internal.gradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.mockito.release.gradle.ReleaseNotesPlugin;
import org.mockito.release.gradle.ReleaseToolsProperties;
import org.mockito.release.internal.gradle.util.ExtContainer;

import java.io.File;

import static java.util.Arrays.asList;
import static org.mockito.release.internal.gradle.util.CommonSettings.TASK_GROUP;

/**
 * --------------------------
 * ******* IMPORTANT ********
 * --------------------------
 *
 * Please update the documentation in the {@link ReleaseNotesPlugin} interface
 * when you make changes to this implementation
 * (for example: adding new tasks, renaming existing tasks, etc.).
 */
public class DefaultReleaseNotesPlugin implements ReleaseNotesPlugin {

    private final static String EXTENSION_NAME = "notes";

    public void apply(final Project project) {
        final DefaultReleaseNotesExtension notes = new DefaultReleaseNotesExtension(project.getProjectDir(), EXTENSION_NAME);

        //TODO those should be task classes with decent API
        project.getTasks().create("updateReleaseNotes", new Action<Task>() {
            public void execute(Task task) {
                task.setGroup(TASK_GROUP);
                task.setDescription("Updates release notes file.");
                task.doLast(new Action<Task>() {
                    public void execute(Task task) {
                        configureNotes(notes, project);
                        notes.updateReleaseNotes(project.getVersion().toString());
                    }
                });
            }
        });

        project.getTasks().create("previewReleaseNotes", new Action<Task>() {
            public void execute(Task task) {
                task.setGroup(TASK_GROUP);
                task.setDescription("Shows new incremental content of release notes. Useful for previewing the release notes.");
                task.doLast(new Action<Task>() {
                    public void execute(Task task) {
                        configureNotes(notes, project);
                        String content = notes.getReleaseNotes(project.getVersion().toString());
                        task.getLogger().lifecycle("----------------\n" + content + "----------------");
                    }
                });
            }
        });

        project.getTasks().create("updateNotableReleaseNotes", NotableReleaseNotesGeneratorTask.class,
                new Action<NotableReleaseNotesGeneratorTask>() {
            public void execute(NotableReleaseNotesGeneratorTask task) {
                final NotableReleaseNotesGeneratorTask.NotesGeneration gen = task.getNotesGeneration();

                //TODO hardcoded
                gen.setGitHubLabels(asList("noteworthy"));
                gen.setDetailedReleaseNotesLink("https://github.com/mockito/mockito-release-tools-example/blob/master/docs/release-notes.md");
                gen.setGitWorkingDir(project.getRootDir());
                gen.setIntroductionText("Notable release notes:\n\n");
                gen.setOnlyPullRequests(true);
                gen.setTagPrefix("v");
                gen.setVcsCommitsLinkTemplate("https://github.com/mockito/mockito-release-tools-example/compare/{0}...{1}");

                task.doFirst(new Action<Task>() {
                    public void execute(Task task) {
                        //lazily configure to give the user chance to specify those settings in build.gradle file
                        configureNotableNotes(project, gen);
                    }
                });
            }
        });
    }

    private static void configureNotableNotes(Project project, NotableReleaseNotesGeneratorTask.NotesGeneration gen) {
        ExtContainer ext = new ExtContainer(project);
        gen.setGitHubReadOnlyAuthToken(ext.getGitHubReadOnlyAuthToken());
        gen.setGitHubRepository(ext.getGitHubRepository());
        gen.setOutputFile(project.file(ext.getNotableReleaseNotesFile()));
    }

    private static void configureNotes(DefaultReleaseNotesExtension notes, Project project) {
        ExtContainer ext = new ExtContainer(project);
        notes.setGitHubLabelMapping(ext.getMap(ReleaseToolsProperties.releaseNotes_labelMapping));
        notes.setGitHubReadOnlyAuthToken(ext.getGitHubReadOnlyAuthToken());
        notes.setGitHubRepository(ext.getString(ReleaseToolsProperties.gh_repository));
        notes.setReleaseNotesFile(project.file(ext.getReleaseNotesFile()));
        notes.assertConfigured();

        //TODO make use of: ext.gh_writeAuthTokenEnvName = "GH_WRITE_TOKEN"
    }
}