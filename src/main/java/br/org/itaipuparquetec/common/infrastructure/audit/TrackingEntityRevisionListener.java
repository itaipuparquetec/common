package br.org.itaipuparquetec.common.infrastructure.audit;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionType;
import org.springframework.security.core.context.SecurityContextHolder;

public class TrackingEntityRevisionListener implements EntityTrackingRevisionListener {

    @Override
    public void newRevision(final Object revisionEntity) {
        ((Revision) revisionEntity).setExternalUserId(getExternalUserId());
    }

    @Override
    public void entityChanged(final Class entityClass, final String entityName, final Object entityId, final RevisionType revisionType, final Object revisionEntity) {
        ((Revision) revisionEntity).setExternalUserId(getExternalUserId());
    }

    private String getExternalUserId() {
        if (SecurityContextHolder.getContext() != null
                && SecurityContextHolder.getContext().getAuthentication() != null) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return null;
    }
}