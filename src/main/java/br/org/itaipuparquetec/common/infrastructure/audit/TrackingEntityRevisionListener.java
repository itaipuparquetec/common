package br.org.itaipuparquetec.common.infrastructure.audit;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionType;

public class TrackingEntityRevisionListener implements EntityTrackingRevisionListener {

    @Override
    public void newRevision(final Object revisionEntity) {
    }

    @Override
    public void entityChanged(Class entityClass, String entityName, Object entityId, RevisionType revisionType, Object revisionEntity) {
    }
}
