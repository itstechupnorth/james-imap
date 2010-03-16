package org.apache.james.imap.functional.jcr;

import org.apache.james.imap.functional.suite.UidSearch;

public class UidSearchTest extends UidSearch{

    public UidSearchTest() throws Exception {
        super(JCRHostSystem.build());
    }
}
