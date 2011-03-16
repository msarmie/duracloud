/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.services.hadoop.fixitymetadata;

import org.duracloud.services.hadoop.base.AltTextOutputFormat;

/**
 * @author: Andrew Woods
 * Date: Feb 9, 2011
 */
public class FixityMetadataOutputFormat extends AltTextOutputFormat {

    @Override
    protected String getOutputFileName() {
        return "bit-integrity-bulk/bit-integrity-metadata-results.csv";
    }

}

