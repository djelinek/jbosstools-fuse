/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.fuse.reddeer.condition;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.swt.impl.button.PushButton;

/**
 * Wait condition which try click a push button with defined label
 *
 * @author djelinek
 *
 */
public class PushButtonIsAvailable extends AbstractWaitCondition {

	private String label;

	public PushButtonIsAvailable(String label) {
		this.label = label;
	}

	@Override
	public boolean test() {
		try {
			new PushButton(label).isEnabled();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
