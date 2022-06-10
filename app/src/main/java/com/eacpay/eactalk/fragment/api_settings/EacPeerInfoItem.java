package com.eacpay.eactalk.fragment.api_settings;

public class EacPeerInfoItem {
    public String addr;
    public String subver;
    public long synced_headers;

    public boolean isLargeVersion(EacPeerInfoItem t2) {
        String[] verCodesT1 = getVerCodes(this);
        String[] verCodesT2 = getVerCodes(t2);
        if (verCodesT1.length != 3 || verCodesT2.length != 3) {
            return false;
        }

        for (int i = 0; i < verCodesT1.length; i++) {
            if (Integer.parseInt(verCodesT1[i]) > Integer.parseInt(verCodesT2[i])) {
                return true;
            }
        }
        return false;
    }

    private String[] getVerCodes(EacPeerInfoItem eacPeerInfoItem) {
        if (!eacPeerInfoItem.subver.contains(":")) {
            return new String[]{"0", "0", "0"};
        }
        String newVer = eacPeerInfoItem.subver.replaceAll("/", "");
        newVer = newVer.split(":")[1];
        return newVer.split("\\.");
    }
}
