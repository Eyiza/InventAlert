package com.inventalert.inventoryService.multicompany;

public class CompanyContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void set(String companyId) {
        CURRENT.set("company_" + companyId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
