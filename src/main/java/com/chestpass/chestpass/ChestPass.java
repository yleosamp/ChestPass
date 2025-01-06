package com.chestpass.chestpass;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ChestPass extends JavaPlugin implements Listener {

    private Map<String, String> bauSenhas = new HashMap<>();
    private Map<String, String> bauDonos = new HashMap<>(); // Agora guarda só o nome
    private Map<Player, StringBuilder> senhasDigitadas = new HashMap<>();
    private Map<Player, Block> bauAtual = new HashMap<>();
    private Map<Player, Boolean> definindoSenha = new HashMap<>();
    private Map<Player, ItemStack[]> inventariosSalvos = new HashMap<>(); // Novo Map

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        carregarDados();
        
        // Salva os dados a cada 5 minutos
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            salvarDados();
        }, 6000L, 6000L); // 6000 ticks = 5 minutos
    }

    private String getLocalizacaoBau(Block bloco) {
        return bloco.getWorld().getName() + "," + bloco.getX() + "," + bloco.getY() + "," + bloco.getZ();
    }

    @EventHandler
    public void aoClicarBau(PlayerInteractEvent evento) {
        if (evento.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (evento.getClickedBlock().getType() != Material.CHEST) return;

        Player jogador = evento.getPlayer();
        Block bau = evento.getClickedBlock();
        String locBau = getLocalizacaoBau(bau);

        if (!bauSenhas.containsKey(locBau)) {
            if (jogador.isSneaking()) {
                if (!jogador.isOp() && jogador.hasPermission("chestpass.use.off")) {
                    jogador.sendMessage(ChatColor.RED + "Você não tem permissão para definir senhas!");
                    evento.setCancelled(true);
                    return;
                }
                evento.setCancelled(true);
                definindoSenha.put(jogador, true);
                bauAtual.put(jogador, bau);
                salvarELimparInventario(jogador);
                abrirMenuSenha(jogador, true);
            }
            return;
        } else {
            evento.setCancelled(true);
            if (bauDonos.get(locBau).equals(jogador.getName())) {
                if (jogador.isSneaking()) {
                    bauSenhas.remove(locBau);
                    bauDonos.remove(locBau);
                    jogador.sendMessage(ChatColor.GREEN + "Senha removida do baú!");
                    return;
                }
            }
            definindoSenha.put(jogador, false);
            bauAtual.put(jogador, bau);
            salvarELimparInventario(jogador);
            abrirMenuSenha(jogador, false);
        }
    }

    private void abrirMenuSenha(Player jogador, boolean definindo) {
        Inventory inv = Bukkit.createInventory(null, 36, definindo ? "Defina a Senha" : "Digite a Senha");
        senhasDigitadas.put(jogador, new StringBuilder());

        for (int i = 0; i <= 9; i++) {
            ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) cabeca.getItemMeta();
            meta.setDisplayName(String.valueOf(i));
            cabeca.setItemMeta(meta);
            inv.setItem(i, cabeca);
        }

        if (!definindo && bauDonos.get(getLocalizacaoBau(bauAtual.get(jogador))).equals(jogador.getName())) {
            ItemStack barreira = new ItemStack(Material.BARRIER);
            ItemMeta meta = barreira.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Remover Senha");
            barreira.setItemMeta(meta);
            inv.setItem(35, barreira);
        }

        jogador.openInventory(inv);
    }

    @EventHandler
    public void aoClicarInventario(InventoryClickEvent evento) {
        String titulo = evento.getView().getTitle();
        if (!titulo.equals("Defina a Senha") && !titulo.equals("Digite a Senha")) return;
        evento.setCancelled(true);

        if (!(evento.getWhoClicked() instanceof Player)) return;
        Player jogador = (Player) evento.getWhoClicked();

        if (evento.getCurrentItem() == null) return;

        if (evento.getCurrentItem().getType() == Material.BARRIER) {
            String locBau = getLocalizacaoBau(bauAtual.get(jogador));
            if (bauDonos.get(locBau).equals(jogador.getName())) {
                bauSenhas.remove(locBau);
                bauDonos.remove(locBau);
                jogador.sendMessage(ChatColor.GREEN + "Senha removida do baú!");
                jogador.closeInventory();
            }
            return;
        }

        if (evento.getCurrentItem().getType() != Material.PLAYER_HEAD) return;

        String numeroClicado = evento.getCurrentItem().getItemMeta().getDisplayName();
        senhasDigitadas.get(jogador).append(numeroClicado);

        // Mostra o número clicado na hotbar
        ItemStack numeroItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) numeroItem.getItemMeta();
        meta.setDisplayName(numeroClicado);
        numeroItem.setItemMeta(meta);
        jogador.getInventory().setItem(senhasDigitadas.get(jogador).length() - 1, numeroItem);

        if (senhasDigitadas.get(jogador).length() >= 4) {
            if (definindoSenha.get(jogador)) {
                definirSenha(jogador);
            } else {
                verificarSenha(jogador);
            }
        }
    }

    private void definirSenha(Player jogador) {
        String senha = senhasDigitadas.get(jogador).toString();
        String locBau = getLocalizacaoBau(bauAtual.get(jogador));
        
        bauSenhas.put(locBau, senha);
        bauDonos.put(locBau, jogador.getName());
        
        jogador.sendMessage(ChatColor.GREEN + "Senha definida com sucesso!");
        limparDados(jogador);
    }

    private void verificarSenha(Player jogador) {
        String senhaDigitada = senhasDigitadas.get(jogador).toString();
        String locBau = getLocalizacaoBau(bauAtual.get(jogador));
        Block bau = bauAtual.get(jogador);
        
        if (senhaDigitada.equals(bauSenhas.get(locBau))) {
            jogador.sendMessage(ChatColor.GREEN + "Senha correta! Baú desbloqueado!");
            restaurarInventario(jogador);
            jogador.closeInventory();
            
            // Abrindo o baú após um tick para garantir que a interface anterior foi fechada
            Bukkit.getScheduler().runTask(this, () -> {
                org.bukkit.block.Chest chest = (org.bukkit.block.Chest) bau.getState();
                Inventory chestInv = chest.getBlockInventory();
                jogador.openInventory(chestInv);
            });
        } else {
            jogador.sendMessage(ChatColor.RED + "Senha incorreta! Tente novamente.");
            restaurarInventario(jogador);
            limparDados(jogador);
        }
        
        // Limpamos os dados apenas depois de usar o baú
        senhasDigitadas.remove(jogador);
        bauAtual.remove(jogador);
        definindoSenha.remove(jogador);
    }

    private void limparDados(Player jogador) {
        restaurarInventario(jogador);
        senhasDigitadas.remove(jogador);
        bauAtual.remove(jogador);
        definindoSenha.remove(jogador);
        jogador.closeInventory();
    }

    private void salvarDados() {
        // Executa o salvamento em uma thread assíncrona para não travar o servidor
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            File file = new File(getDataFolder(), "data.yml");
            YamlConfiguration config = new YamlConfiguration();
            
            // Salvando senhas dos baús
            for (Map.Entry<String, String> entry : bauSenhas.entrySet()) {
                config.set("senhas." + entry.getKey(), entry.getValue());
            }
            
            // Salvando donos dos baús
            for (Map.Entry<String, String> entry : bauDonos.entrySet()) {
                config.set("donos." + entry.getKey(), entry.getValue());
            }
            
            try {
                if (!file.exists()) {
                    getDataFolder().mkdirs();
                    file.createNewFile();
                }
                config.save(file);
            } catch (IOException e) {
                getLogger().warning("Erro ao salvar dados: " + e.getMessage());
            }
        });
    }

    private void carregarDados() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        File file = new File(getDataFolder(), "data.yml");
        if (!file.exists()) {
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // Carregando senhas
        ConfigurationSection senhasSection = config.getConfigurationSection("senhas");
        if (senhasSection != null) {
            for (String key : senhasSection.getKeys(false)) {
                bauSenhas.put(key, senhasSection.getString(key));
            }
        }
        
        // Carregando donos
        ConfigurationSection donosSection = config.getConfigurationSection("donos");
        if (donosSection != null) {
            for (String key : donosSection.getKeys(false)) {
                bauDonos.put(key, donosSection.getString(key));
            }
        }
    }

    @Override
    public void onDisable() {
        salvarDados();
    }

    // Novos métodos para gerenciar inventário
    private void salvarELimparInventario(Player jogador) {
        inventariosSalvos.put(jogador, jogador.getInventory().getContents());
        jogador.getInventory().clear();
    }

    private void restaurarInventario(Player jogador) {
        if (inventariosSalvos.containsKey(jogador)) {
            jogador.getInventory().setContents(inventariosSalvos.get(jogador));
            inventariosSalvos.remove(jogador);
        }
    }
}
