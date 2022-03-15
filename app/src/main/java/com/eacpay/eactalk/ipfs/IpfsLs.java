package com.eacpay.eactalk.ipfs;

import java.util.List;

public class IpfsLs {
    public List<IpfsObject> Objects;

    public String getFirstName() {
        if (Objects.size() == 0) {
            return "";
        }
        if (Objects.get(0).Links.size() == 0) {
            return "";
        }
        IpfsLink link = Objects.get(0).Links.get(0);
        return link.Name;
    }

    public String getFirstIpfsCid() {
        IpfsLink link = Objects.get(0).Links.get(0);
        return link.Hash;
    }

    public boolean isDir() {
        return getFirstName() != null && !getFirstName().equals("");
    }
}