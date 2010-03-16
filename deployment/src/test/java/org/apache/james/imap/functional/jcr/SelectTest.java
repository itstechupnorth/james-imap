package org.apache.james.imap.functional.jcr;

import org.apache.james.imap.functional.suite.Select;

public class SelectTest extends Select{

    public SelectTest() throws Exception {
        super(JCRHostSystem.build());
    }
}
