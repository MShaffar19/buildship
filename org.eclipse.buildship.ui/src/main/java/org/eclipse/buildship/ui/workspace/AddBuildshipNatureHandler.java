package org.eclipse.buildship.ui.workspace;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.gradleware.tooling.toolingclient.GradleDistribution;
import com.gradleware.tooling.toolingmodel.OmniEclipseProject;
import com.gradleware.tooling.toolingmodel.repository.FixedRequestAttributes;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.buildship.core.CorePlugin;
import org.eclipse.buildship.core.configuration.GradleProjectNature;
import org.eclipse.buildship.core.workspace.CompositeGradleBuild;
import org.eclipse.buildship.core.workspace.NewProjectHandler;

/**
 * Synchronizes the given projects as if the user had run the import wizard on their location.
 *
 * @author Stefan Oehme
 *
 */
public class AddBuildshipNatureHandler extends AbstractHandler{

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof StructuredSelection) {
            List<?> elements = ((StructuredSelection) selection).toList();
            Set<FixedRequestAttributes> builds = collectGradleBuilds(elements);
            synchronize(builds);
        }
        return null;
    }

    private Set<FixedRequestAttributes> collectGradleBuilds(List<?> elements) {
        Set<FixedRequestAttributes> builds = Sets.newLinkedHashSet();
        for (Object element : elements) {
            IProject project = Platform.getAdapterManager().getAdapter(element, IProject.class);
            if (project != null && !GradleProjectNature.isPresentOn(project)) {
                IPath location = project.getLocation();
                if (location != null) {
                    builds.add(new FixedRequestAttributes(location.toFile(), null, GradleDistribution.fromBuild(), null, Lists.<String>newArrayList(), Lists.<String>newArrayList()));
                }
            }

        }
        return builds;
    }

    private void synchronize(Set<FixedRequestAttributes> builds) {
        CompositeGradleBuild compositeBuild = CorePlugin.gradleWorkspaceManager().getCompositeBuild();
        final Set<File> projectDirectories = Sets.newHashSet();
        for (FixedRequestAttributes build : builds) {
            compositeBuild = compositeBuild.withBuild(build);
            projectDirectories.add(build.getProjectDir());
        }
        compositeBuild.synchronize(new NewProjectHandler() {

            @Override
            public boolean shouldOverwriteDescriptor(IProjectDescription descriptor, OmniEclipseProject projectModel) {
                return false;
            }

            @Override
            public boolean shouldImport(OmniEclipseProject projectModel) {
                return projectDirectories.contains(projectModel.getRoot().getProjectDirectory());
            }

            @Override
            public void afterImport(IProject project, OmniEclipseProject projectModel) {
            }
        });
    }

}