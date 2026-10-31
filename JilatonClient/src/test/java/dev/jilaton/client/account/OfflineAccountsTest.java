package dev.jilaton.client.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineAccountsTest {
    @TempDir Path dir;

    @Test
    void namesFollowMinecraftsRules() {
        assertTrue(OfflineAccounts.valid("Zaber_01"));
        assertFalse(OfflineAccounts.valid("ab"));
        assertFalse(OfflineAccounts.valid("a".repeat(17)));
        assertFalse(OfflineAccounts.valid("bad name"));
        assertFalse(OfflineAccounts.valid("café"));
    }

    @Test
    void addRenameRemovePersistAndRejectDuplicates() throws Exception {
        Path file = dir.resolve("jilaton/accounts.txt");
        OfflineAccounts accounts = new OfflineAccounts(file);
        assertTrue(accounts.add("Builder"));
        assertFalse(accounts.add("builder"), "case-insensitive duplicate");
        assertEquals("Already added", accounts.problem("BUILDER"));
        assertTrue(accounts.rename("Builder", "Miner_2"));
        assertEquals(List.of("Miner_2"), new OfflineAccounts(file).names());
        accounts.remove("Miner_2");
        assertTrue(new OfflineAccounts(file).names().isEmpty());
    }

    @Test
    void storesNamesOnly() throws Exception {
        Path file = dir.resolve("accounts.txt");
        new OfflineAccounts(file).add("Player_1");
        assertEquals("Player_1", Files.readString(file).strip());
    }
}
