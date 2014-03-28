package org.duracloud.audit;



/**
 * An intereface defining a log item.
 * 
 * @author Daniel Bernstein
 * 
 */
public interface AuditLogItem {
    /**
     * 
     * @return
     */
    public String getAccount();

    /**
     * 
     * @return
     */
    public String getStoreId();

    /**
     * 
     * @return
     */
    public String getSpaceId();

    /**
     * 
     * @return
     */
    public String getContentId();

    /**
     * 
     * @return
     */
    public String getContentMd5();

    /**
    * @return
    */
   public String getMimetype();

   /*
    * 
    * @return
    */
   public String getContentSize();

   /**
    * 
    * @return
    */
   public String getContentProperties();

   /**
    * 
    * @return
    */
   public String getSpaceAcls();
   
   /**
     * 
     * @return
     */
    public String getAction();

    /**
     * 
     * @return
     */
    public String getUsername();
    
    /**
     * 
     * @return
     */
    public String getSourceSpaceId();

    /**
     * 
     * @return
     */
    public String getSourceContentId();
    /**
     * 
     * @return
     */
    public long getTimestamp();
}