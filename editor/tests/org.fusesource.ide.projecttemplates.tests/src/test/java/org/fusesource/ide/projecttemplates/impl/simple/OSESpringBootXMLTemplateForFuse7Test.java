/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.projecttemplates.impl.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class OSESpringBootXMLTemplateForFuse7Test {

	@Parameter
	public String version;
	
	@Parameter(1)
	public boolean isCompatible;
	
	@Parameters(name = "{0} should be compatible? {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { 
			{ "2.21.0", true },
			{ "2.20.0", true },
			{ "2.19.9", false },
		});
	}
	
	@Test
	public void testIsCompatible() throws Exception {
		assertThat(new OSESpringBootXMLTemplateForFuse7().isCompatible(version)).isEqualTo(isCompatible);
	}

}