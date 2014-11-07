/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.auditor.jpa;

import java.util.Date;
import java.util.Iterator;
import java.util.List;


import org.duracloud.audit.AuditLogItem;
import org.duracloud.audit.AuditLogStore;
import org.duracloud.audit.AuditLogWriteFailedException;
import org.duracloud.common.collection.StreamingIterator;
import org.duracloud.error.NotFoundException;
import org.duracloud.mill.db.model.JpaAuditLogItem;
import org.duracloud.mill.db.repo.JpaAuditLogItemRepo;
import org.duracloud.mill.db.repo.MillJpaRepoConfig;
import org.duracloud.mill.db.util.JpaIteratorSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
@Transactional(value=MillJpaRepoConfig.TRANSACTION_MANAGER_BEAN)
public class JpaAuditLogStore implements AuditLogStore {

    private JpaAuditLogItemRepo auditLogRepo;

    @Autowired
    public JpaAuditLogStore(JpaAuditLogItemRepo auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }

    @Override
    public void write(String account,
                      String storeId,
                      String spaceId,
                      String contentId,
                      String contentMd5,
                      String mimetype,
                      String contentSize,
                      String user,
                      String action,
                      String properties,
                      String spaceAcls,
                      String sourceSpaceId,
                      String sourceContentId,
                      Date timestamp) throws AuditLogWriteFailedException {

        JpaAuditLogItem item = new JpaAuditLogItem();

        try {
            item.setAccount(account);
            item.setStoreId(storeId);
            item.setSpaceId(spaceId);
            item.setContentId(contentId);
            item.setContentMd5(contentMd5);
            item.setMimetype(mimetype);
            item.setContentSize(contentSize);
            item.setUsername(user);
            item.setAction(action);
            item.setContentProperties(properties);
            item.setSpaceAcls(spaceAcls);
            item.setSourceSpaceId(sourceSpaceId);
            item.setSourceContentId(sourceContentId);
            item.setTimestamp(timestamp.getTime());
            this.auditLogRepo.saveAndFlush(item);

        } catch (Exception ex) {
            throw new AuditLogWriteFailedException(ex, item);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly=true)
    public Iterator<AuditLogItem> getLogItems(final String account,
            final String spaceId) {
        return (Iterator) new StreamingIterator<JpaAuditLogItem>(
                new JpaIteratorSource<JpaAuditLogItemRepo, JpaAuditLogItem>(
                        auditLogRepo) {
                    @Override
                    protected Page getNextPage(
                            Pageable pageable, JpaAuditLogItemRepo repo) {
                        return repo.findByAccountAndSpaceIdOrderByContentIdAsc(
                                account, spaceId, pageable);
                    }
                });
    }

    @Override
    @Transactional(readOnly=true)
    public Iterator<AuditLogItem> getLogItems(final String account,
                                              final String storeId,
                                              final String spaceId,
                                              final String contentId) {
        return (Iterator) new StreamingIterator<JpaAuditLogItem>(new JpaIteratorSource<JpaAuditLogItemRepo, JpaAuditLogItem>(auditLogRepo) {
            @Override
            protected Page<JpaAuditLogItem> getNextPage(Pageable pageable, JpaAuditLogItemRepo repo) {
                return repo
                        .findByAccountAndStoreIdAndSpaceIdAndContentIdOrderByContentIdAsc(account,
                                                                                          storeId,
                                                                                          spaceId,
                                                                                          contentId,
                                                                                          pageable);
            }
        });
    }

    @Override
    @Transactional(readOnly=true)
    public AuditLogItem
            getLatestLogItem(String account,
                             String storeId,
                             String spaceId,
                             String contentId) throws NotFoundException {
        List<JpaAuditLogItem> result = auditLogRepo
                .findByAccountAndStoreIdAndSpaceIdAndContentIdOrderByTimestampDesc(account,
                                                                                   storeId,
                                                                                   spaceId,
                                                                                   contentId);
        if(!CollectionUtils.isEmpty(result)){
            return result.get(0);
        }else{
            return null;
        }
    }

    @Override
    @Transactional(readOnly=true)
    public void updateProperties(AuditLogItem item, String properties)
            throws AuditLogWriteFailedException {
        
        if(!(item instanceof JpaAuditLogItem)){
            throw new AuditLogWriteFailedException("audit log item must be of type " +
                                                   "JpaAuditLogItem when used with this " +
                                                   "implementation: item is of type "
                                                   + item.getClass().getCanonicalName(), item);
        }
        Long id = ((JpaAuditLogItem)item).getId();
        
        JpaAuditLogItem refreshedItem = auditLogRepo.findOne(id); 
        refreshedItem.setContentProperties(properties);
        auditLogRepo.saveAndFlush(refreshedItem);
    }

}