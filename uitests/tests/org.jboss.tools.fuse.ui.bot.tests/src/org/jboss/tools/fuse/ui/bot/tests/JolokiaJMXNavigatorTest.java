/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.fuse.ui.bot.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.direct.preferences.PreferencesUtil;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasText;
import org.eclipse.reddeer.eclipse.debug.ui.launchConfigurations.RunConfigurationsDialog;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.ui.views.log.LogView;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement.CleanWorkspace;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.menu.ShellMenuItem;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.jboss.tools.fuse.reddeer.LogGrapper;
import org.jboss.tools.fuse.reddeer.ProjectTemplate;
import org.jboss.tools.fuse.reddeer.ProjectType;
import org.jboss.tools.fuse.reddeer.SupportedCamelVersions;
import org.jboss.tools.fuse.reddeer.condition.JMXConnectionIsAvailable;
import org.jboss.tools.fuse.reddeer.launchconfigurations.LocalCamelContextLaunchConfiguration;
import org.jboss.tools.fuse.reddeer.launchconfigurations.LocalCamelContextLaunchConfigurationJRETab;
import org.jboss.tools.fuse.reddeer.perspectives.FuseIntegrationPerspective;
import org.jboss.tools.fuse.reddeer.projectexplorer.CamelProject;
import org.jboss.tools.fuse.reddeer.view.FuseJMXNavigator;
import org.jboss.tools.fuse.reddeer.wizard.JMXNewConnectionWizard;
import org.jboss.tools.fuse.reddeer.wizard.JMXNewConnectionWizard.ConnectionType;
import org.jboss.tools.fuse.ui.bot.tests.utils.ProjectFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@CleanWorkspace
@OpenPerspective(FuseIntegrationPerspective.class)
@RunWith(RedDeerSuite.class)
public class JolokiaJMXNavigatorTest {

	private static final String PROJECT_NAME = "camel-blueprint";
	private static final String PROJECT_CAMEL_CONTEXT = "src/main/resources/OSGI-INF/blueprint/blueprint.xml";
	private static final String JOLOKIA_CONNECTION_NAME = "test-blueprint-connection";

	private static Logger log = Logger.getLogger(JMXNavigatorTest.class);

	/**
	 * Prepares test environment
	 */
	@BeforeClass
	public static void setupCreateProject() {
		new WorkbenchShell().maximize();
		log.info("Disable showing Console view after standard output changes");
		PreferencesUtil.setConsoleOpenedOnError(false);
		PreferencesUtil.setConsoleOpenedOnOutput(false);
		log.info("Disable showing Error Log view after changes");
		new LogView().open();
		new LogView().setActivateOnNewEvents(false);
		log.info("Create a new Fuse project from 'Content Based Router' template");
		ProjectFactory.newProject(PROJECT_NAME).version(SupportedCamelVersions.CAMEL_LATEST)
				.template(ProjectTemplate.CBR).type(ProjectType.BLUEPRINT).create();
		new CamelProject(PROJECT_NAME).update();
	}

	/**
	 * Prepares test environment
	 */
	@Before
	public void defaultSetup() {
		new WorkbenchShell();
		new LogView().deleteLog();
	}

	/**
	 * Cleans up test environment
	 */
	@After
	public void defaultClean() {
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
		log.info("Save editor");
		ShellMenuItem menuItem = new ShellMenuItem(new WorkbenchShell(), "File", "Save All");
		if (menuItem.isEnabled()) {
			menuItem.select();
		}
	}

	/**
	 * Cleans up test environment
	 */
	@AfterClass
	public static void defaultFinalClean() {
		new WorkbenchShell();
		log.info("Try to terminate a console.");
		ConsoleView console = new ConsoleView();
		console.open();
		if (console.canTerminateConsole())
			console.terminateConsole();
		ProjectFactory.deleteAllProjects();
	}

	/**
	 * <p>
	 * Create a new Fuse integration Project (CBR, blueprint, the latest Camel version).
	 * </p>
	 * <ul>
	 * <li>Run as Local Camel Context (to create a launch configuration)</li>
	 * <li>Stop the running Local Camel Context</li>
	 * <li>Go to Run --> Run Configurations... --> Local Camel Context --> select created launch configuration</li>
	 * <li>Switch to JRE tab and add new VM argument: <i>-javaagent:path/to/jolokia-jvm-1.3.7-agent.jar</i></li>
	 * <li>Apply changes and hit Run button</li>
	 * <li>Notice: you should get something like Jolokia: Agent started with URL <i>http://127.0.0.1:8778/jolokia/</i>
	 * in Console View</li>
	 * <li>Create a new connection in JMX Navigator</li>
	 * <li>New connection... --> <i>Jolokia Connection</i></li>
	 * <li>Provide the name you want</li>
	 * <li>Use Jolokia URL from Console View (by default <i>http://127.0.0.1:8778/jolokia</i>)</li>
	 * <li>Select <i>Do NOT verify SSL Certificates</i></li>
	 * <li>Click Finish</li>
	 * <li>Expand User-Defined Connections --> double-click on the connection to activate it</li>
	 * <li>Try to work with it (accessing nodes, perform available operations, ...)</li>
	 * </ul>
	 * @throws Exception 
	 */
	@Test
	public void testJolokiaAcccessingCamelRoute() throws Exception {
		CamelProject project = new CamelProject(PROJECT_NAME);

		log.info("Run the Fuse project as Local Camel Context");
		project.runCamelContext();

		log.info("Try to stop running local camel context via terminate a console.");
		ConsoleView console = new ConsoleView();
		console.open();
		if (console.canTerminateConsole())
			console.terminateConsole();

		log.info(
				" Try to open 'Run -> Run Configurations...' and select created configuration for Local Camel Context");
		RunConfigurationsDialog run = new RunConfigurationsDialog();
		project.update();
		project.selectProjectItem("src/main/resources", "OSGI-INF", "blueprint", PROJECT_CAMEL_CONTEXT);
		new ContextMenuItem("Run As", "Run Configurations...").select();
		new WaitUntil(new ShellIsAvailable("Run Configurations"));
		run.select(new LocalCamelContextLaunchConfiguration(),
				"Run " + PROJECT_NAME + " (blueprint.xml) as Local CamelContext");

		log.info("Try to set VM argument for Jolokia jvm agent and run local camel context");
		LocalCamelContextLaunchConfigurationJRETab jre = new LocalCamelContextLaunchConfigurationJRETab();
		jre.activate();
		jre.setTextVMArgumentsTXT("-javaagent:" + System.getProperty("jolokia"));
		jre.clickApplyBTN();
		run.run();
		new WaitUntil(new ConsoleHasText("Starting Camel ..."), TimePeriod.LONG);

		log.info("Try to open/activate 'JMX Navigator' view");
		FuseJMXNavigator jmxView = new FuseJMXNavigator();
		jmxView.activate();
		jmxView.clickNewConnection();

		log.info("Try to create a new jolokia connection");
		JMXNewConnectionWizard connectionWizard = new JMXNewConnectionWizard("Create JMX Connection");
		new WaitUntil(new ShellIsAvailable("Create JMX Connection"));
		assertTrue("Wizard 'Create JMX connection' was not opened properly", connectionWizard.isOpen());
		connectionWizard.selectConnection(ConnectionType.JOLOKIA);
		connectionWizard.next();
		connectionWizard.setTextConnectionName(JOLOKIA_CONNECTION_NAME);
		connectionWizard.setTextJolokiaURL("http://127.0.0.1:8778/jolokia/");
		connectionWizard.getDoNOTVerifySSLCertificatesDangerousForLocalUseOnlyCHB().click();
		connectionWizard.finish();

		log.info("Try to connect to the new jolokia connection");
		jmxView.connectTo("User-Defined Connections", JOLOKIA_CONNECTION_NAME);
		new WaitUntil(
				new JMXConnectionIsAvailable("User-Defined Connections", JOLOKIA_CONNECTION_NAME + "[Connected]"));

		log.info("Try to access route components via new Jolokia connection");
		assertNotNull(
				"The following path is inaccesible: User-Defined connections/" + JOLOKIA_CONNECTION_NAME
						+ "[Connected]/MBeans/java.util.logging/Logging/LoggerNames",
				jmxView.getNode("User-Defined Connections", JOLOKIA_CONNECTION_NAME + "[Connected]", "MBeans",
						"java.util.logging", "Logging", "LoggerNames"));
		jmxView.collapseAll();
		assertNotNull(
				"The following path is inaccesible: User-Defined connections/" + JOLOKIA_CONNECTION_NAME
						+ "[Connected]/Camel/camelContext-.../Endpoints/file/work/cbr/input",
				jmxView.getNode("User-Defined Connections", JOLOKIA_CONNECTION_NAME + "[Connected]", "Camel",
						"cbr-example-context", "Endpoints", "file", "work/cbr/input"));
		jmxView.collapseAll();
		assertNotNull("The following path is inaccesible: User-Defined connections/" + JOLOKIA_CONNECTION_NAME
				+ "[Connected]/Camel/camelContext-.../Routes/cbr-route/file:work/cbr/input/Log _log1/Choice/Otherwise/Log _log4/file:work/cbr/output/others",
				jmxView.getNode("User-Defined Connections", JOLOKIA_CONNECTION_NAME + "[Connected]", "Camel",
						"cbr-example-context", "Routes", "cbr-route", "file:work/cbr/input", "Log _log1", "Choice",
						"Otherwise", "Log _log4", "file:work/cbr/output/others"));
		jmxView.collapseAll();
		assertTrue("There are some errors in Error Log", LogGrapper.getPluginErrors("fuse").size() == 0);
	}

}
