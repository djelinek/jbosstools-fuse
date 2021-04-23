/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at https://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.fuse.ui.bot.tests;

import static org.jboss.tools.fuse.reddeer.wizard.NewFuseIntegrationProjectWizardDeploymentType.STANDALONE;
import static org.jboss.tools.fuse.reddeer.wizard.NewFuseIntegrationProjectWizardRuntimeType.KARAF;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.reddeer.common.matcher.RegexMatcher;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.direct.project.Project;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasText;
import org.eclipse.reddeer.eclipse.condition.LaunchIsSuspended;
import org.eclipse.reddeer.eclipse.debug.ui.views.launch.TerminateButton;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.ui.views.log.LogView;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.cleanerrorlog.CleanErrorLogRequirement.CleanErrorLog;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement.CleanWorkspace;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.jboss.tools.fuse.reddeer.ProjectTemplate;
import org.jboss.tools.fuse.reddeer.debug.StepOverButton;
import org.jboss.tools.fuse.reddeer.editor.CamelEditor;
import org.jboss.tools.fuse.reddeer.preference.ConsolePreferenceUtil;
import org.jboss.tools.fuse.reddeer.projectexplorer.CamelProject;
import org.jboss.tools.fuse.reddeer.utils.LogChecker;
import org.jboss.tools.fuse.reddeer.utils.ProjectFactory;
import org.jboss.tools.fuse.reddeer.view.FuseJMXNavigator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author fpospisi
 */
@CleanWorkspace
@CleanErrorLog
@RunWith(RedDeerSuite.class)
public class EditRoutesDebuggingTest {
	
	private static final String PROJECT_NAME = "EditRoutesDebuggingTest";

	/*
	 * Create new project.
	 */
	@BeforeClass
	public static void setWorkspace() {
		new WorkbenchShell().maximize();
		ConsolePreferenceUtil.setConsoleOpenOnError(false);
		ConsolePreferenceUtil.setConsoleOpenOnOutput(false);
		new LogView().open();
		new LogView().setActivateOnNewEvents(false);

		ProjectFactory.newProject(PROJECT_NAME).deploymentType(STANDALONE).runtimeType(KARAF)
				.template(ProjectTemplate.CBR_BLUEPRINT).create();
		new CamelProject(PROJECT_NAME).update();
	}
	
//	@After
//	public void setupCleanup() {
//		ConsoleView console = new ConsoleView();
//		console.open();
//		try {
//			console.terminateConsole();
//			new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
//		} catch (CoreLayerException ex) {
//		}
//
//		try {
//			new DefaultEditor(new RegexMatcher("<connected>Remote CamelContext:.*")).close();
//		} catch (Exception e) {
//			// editor is not opened --> ok
//		}
//	}

	@AfterClass
	public static void cleanWorkspace() {
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
		new CleanWorkspaceRequirement().fulfill();
	}
	
	/**
	 * <p>
	 * Debugging in "Edit Routes" mode
	 * </p>
	 * <b>Steps</b>
	 * <ol>
	 * <li>Create new project: STANDALONE - KARAF - CBR_BLUEPRINT.</li>
	 * <li>Run as Local Camel Context.</li>
	 * <li>In JMX Navigator connect to maven.</li>
	 * <li>Start "Edit Routes" for Local Processes - maven - Camel - cbr-example-content - Routes.</li>
	 * <li>Change message for "Log _log1" to "AAA-BBB-CCC"</li>
	 * <li>Add breakpoint for "Log _log1".</li>
	 * <li>Send file order1.xml on the Camel route input.</li>
	 * <li>Change perspective to Debug.</li>
	 * <li>Step over breakpoint.</li>
	 * <li>Check log for edited message.</li>
	 * <li>Check log for Fuse related errors.</li>
	 * </ol>
	 */
	@Test
	public void testRouteContextSupport() throws IOException {

		/*
		 * Run as Local Camel Context.
		 */
		new CamelProject(PROJECT_NAME).runCamelContext();		
		assertTrue("Route cbr-route was not property started.",
				new ConsoleView().getConsoleText().contains("cbr-route started and consuming"));
		
		/*
		 * Connect to Maven.
		 */
		FuseJMXNavigator nav = new FuseJMXNavigator();
		nav.refreshLocalProcesses();
		nav.getNode("Local Processes", "Local Camel Context", "Camel");
	
		/*
		 * Open Routes with Edit Routes.
		 */
		TreeItem jmxNode = nav.getNode("Local Processes", "Local Camel Context", "Camel", "cbr-example-context");
		jmxNode.select();
		new ContextMenu(jmxNode).getItem("Edit Routes").select();

//		nav.getNode("Local Processes", "Local Camel Context", "Camel", "cbr-example-context").select();
//		new ContextMenuItem("Edit Routes").select();
//		new WaitUntil(new ConsoleHasText("Enabling debugger"), TimePeriod.LONG);

		/*
		 * Open in editor and check for correct open.
		 */
		CamelEditor editor = new CamelEditor(new DefaultEditor(new RegexMatcher("<connected>Remote CamelContext:.*")).getTitle());
		assertTrue("Component Log _log1 not found.", editor.isComponentAvailable("Log _log1"));
		
		/*
		 * Set breakpoint and change message of Log _log1.  
		 */
		editor.setBreakpoint("Log _log1");	
		new WaitUntil(new ConsoleHasText("Adding breakpoint _log1"), TimePeriod.VERY_LONG);
		editor.selectEditPart("Route cbr-route"); 
		editor.selectEditPart("Log _log1");
		editor.setProperty("Message *", "AAA-BBB-CCC");
		editor.save();
		
		/*
		 * Put order1.xml to input of Camel route.
		 */
		String location = Project.getLocation(PROJECT_NAME);		
		Files.copy(new File(location + "/src/main/data/order1.xml").toPath(),
				new File(location + "/work/cbr/input/order1.xml").toPath());

		/*
		 * Switch to Debug perspective.
		 */
		new WaitUntil(new ShellIsAvailable("Confirm Perspective Switch"), TimePeriod.VERY_LONG);
		new PushButton("Switch").click();
	
		/*
		 * Step over breakpoint.
		 * Check for changed log message.
		 * Check for correct processing of order1.xml.
		 */
		new WaitUntil(new LaunchIsSuspended(), TimePeriod.VERY_LONG);
		new StepOverButton().select();		
		new WaitUntil(new ConsoleHasText("AAA-BBB-CCC"), TimePeriod.VERY_LONG);


		/*
		 * Check for Fuse related errors.
		 */
		LogChecker.assertNoFuseError();
	}

}
