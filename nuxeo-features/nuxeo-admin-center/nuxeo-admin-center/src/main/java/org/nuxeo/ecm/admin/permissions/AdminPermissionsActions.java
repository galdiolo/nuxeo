/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.admin.permissions;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.4
 */
@Name("adminPermissionsActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class AdminPermissionsActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static String PERMISSIONS_PURGE_CONTENT_VIEW = "PERMISSIONS_PURGE";

    public static final String CATEGORY_PURGE_WORK = "permissionsPurge";

    @In(create = true)
    protected ContentViewActions contentViewActions;

    protected String purgeWorkId;

    public void doPurge() {
        ContentView contentView = contentViewActions.getContentView(PERMISSIONS_PURGE_CONTENT_VIEW);
        DocumentModel searchDocumentModel = contentView.getSearchDocumentModel();
        PermissionsPurgeWork work = new PermissionsPurgeWork(searchDocumentModel);
        purgeWorkId = work.getId();
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(work, WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
    }

    public void cancelPurge() {
        ContentView contentView = contentViewActions.getContentView(PERMISSIONS_PURGE_CONTENT_VIEW);
        contentView.resetSearchDocumentModel();
        purgeWorkId = null;
    }

    public boolean canStartPurge() {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        return workManager.getQueueSize("permissionsPurge", Work.State.RUNNING) <= 0;
    }

    public PurgeWorkStatus getPurgeStatus() {
        if (purgeWorkId == null) {
            return null;
        }

        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        Work.State workState = workManager.getWorkState(purgeWorkId);
        if (workState == null) {
            return null;
        }
        return new PurgeWorkStatus(workState);
    }

    public static class PurgeWorkStatus {

        private final Work.State state;

        public PurgeWorkStatus(Work.State state) {
            this.state = state;
        }

        public Work.State getState() {
            return state;
        }

        public boolean isScheduled() {
            return state == Work.State.SCHEDULED;
        }

        public boolean isRunning() {
            return state == Work.State.RUNNING;
        }

        public boolean isComplete() {
            return state == Work.State.COMPLETED;
        }
    }
}