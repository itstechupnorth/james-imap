package org.apache.james.imap.jcr;

import org.apache.james.imap.functional.suite.Rename;

public class RenameTest extends Rename {

    public RenameTest() throws Exception {
        super(JCRHostSystem.build());
    }

}

