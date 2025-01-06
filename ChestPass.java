package com.chestpass.chestpass;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public class ChestPass extends JavaPlugin implements Listener {

    private Map<String, String> bauSenhas = new HashMap<>();
    private Map<String, String> bauDonos = new HashMap<>(); // Agora guarda só o nome
    private Map<Player, StringBuilder> senhasDigitadas = new HashMap<>();
    private Map<Player, Block> bauAtual = new HashMap<>();
    private Map<Player, Boolean> definindoSenha = new HashMap<>();
    private Map<Player, ItemStack[]> inventariosSalvos = new HashMap<>(); // Novo Map
    private Map<Player, Boolean> senhaDefinidaComSucesso = new HashMap<>();
    private YamlConfiguration messages;
    private String language;

    @Override
    public void onEnable() {
        saveResource("messages.yml", false);
        loadMessages();
        getServer().getPluginManager().registerEvents(this, this);
        carregarDados();
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            salvarDados();
        }, 6000L, 6000L);
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        language = messages.getString("language", "en");
        
        // Normaliza o idioma para pt-br se necessário
        if (language.equalsIgnoreCase("pt") || language.equalsIgnoreCase("br")) {
            language = "pt-br";
        }
    }

    private String getMessage(String key) {
        String path = "messages." + language + "." + key;
        String message = messages.getString(path);
        if (message == null) {
            message = messages.getString("messages.en." + key, "Message not found: " + key);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
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
                    jogador.sendMessage(getMessage("no-permission"));
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
            if (bauDonos.get(locBau).equals(jogador.getName())) {
                if (jogador.isSneaking()) {
                    bauSenhas.remove(locBau);
                    bauDonos.remove(locBau);
                    jogador.sendMessage(getMessage("password-removed"));
                    return;
                }
                return;
            }
            evento.setCancelled(true);
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
            meta.setDisplayName(ChatColor.GOLD + "[ " + ChatColor.YELLOW + i + ChatColor.GOLD + " ]");
            
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                URL url;
                if (i % 2 == 0) {
                    // Cabeça Vermelha
                    url = new URL("http://textures.minecraft.net/texture/ba24a2b6b4b5a92d7a82a373fe5f6bb66872ead66c126f82e8864173cd783a");
                } else {
                    // Cabeça Verde
                    url = new URL("http://textures.minecraft.net/texture/921928ea67d3a8b97d212758f15cccac1024295b185b319264844f4c5e1e61e");
                }
                textures.setSkin(url);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } catch (Exception e) {
                getLogger().warning("Erro ao aplicar textura na cabeça: " + e.getMessage());
            }
            
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

        String numeroClicado = ChatColor.stripColor(evento.getCurrentItem().getItemMeta().getDisplayName().replaceAll("[\\[\\]\\s]", ""));
        senhasDigitadas.get(jogador).append(numeroClicado);

        // Copia a cabeça clicada exatamente como está
        ItemStack numeroItem = evento.getCurrentItem().clone();
        ItemMeta meta = numeroItem.getItemMeta();
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
        
        senhaDefinidaComSucesso.put(jogador, true);
        jogador.sendMessage(getMessage("password-set"));
        restaurarInventario(jogador);
        jogador.closeInventory();
        limparDados(jogador);
    }

    private void verificarSenha(Player jogador) {
        String senhaDigitada = senhasDigitadas.get(jogador).toString();
        String locBau = getLocalizacaoBau(bauAtual.get(jogador));
        Block bau = bauAtual.get(jogador);
        
        if (senhaDigitada.equals(bauSenhas.get(locBau))) {
            jogador.sendMessage(getMessage("password-correct"));
            restaurarInventario(jogador);
            jogador.closeInventory();
            
            Bukkit.getScheduler().runTask(this, () -> {
                org.bukkit.block.Chest chest = (org.bukkit.block.Chest) bau.getState();
                Inventory chestInv = chest.getBlockInventory();
                jogador.openInventory(chestInv);
            });
            limparDados(jogador);
        } else {
            jogador.sendMessage(getMessage("password-incorrect"));
            senhasDigitadas.put(jogador, new StringBuilder());
            for (int i = 0; i < 9; i++) {
                jogador.getInventory().setItem(i, null);
            }
        }
    }

    private void limparDados(Player jogador) {
        senhasDigitadas.remove(jogador);
        bauAtual.remove(jogador);
        definindoSenha.remove(jogador);
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

    @EventHandler
    public void aoFecharInventario(InventoryCloseEvent evento) {
        String titulo = evento.getView().getTitle();
        if (!titulo.equals("Defina a Senha") && !titulo.equals("Digite a Senha")) return;
        
        if (!(evento.getPlayer() instanceof Player)) return;
        Player jogador = (Player) evento.getPlayer();
        
        if (senhaDefinidaComSucesso.remove(jogador) != null) {
            return;
        }
        
        if (senhasDigitadas.containsKey(jogador)) {
            if (definindoSenha.get(jogador)) {
                restaurarInventario(jogador);
                limparDados(jogador);
                jogador.sendMessage(getMessage("password-cancelled"));
            } else {
                jogador.sendMessage(getMessage("password-incorrect"));
                restaurarInventario(jogador);
                limparDados(jogador);
            }
            inventariosSalvos.remove(jogador);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoReceberItem(InventoryClickEvent evento) {
        if (!(evento.getWhoClicked() instanceof Player)) return;
        Player jogador = (Player) evento.getWhoClicked();
        
        if (estaDigitandoSenha(jogador) && !evento.getView().getTitle().equals("Defina a Senha") 
            && !evento.getView().getTitle().equals("Digite a Senha")) {
            evento.setCancelled(true);
            jogador.sendMessage(getMessage("inventory-locked"));
            return;
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void aoArrastarItem(InventoryDragEvent evento) {
        if (!(evento.getWhoClicked() instanceof Player)) return;
        Player jogador = (Player) evento.getWhoClicked();
        
        if (estaDigitandoSenha(jogador)) {
            evento.setCancelled(true);
            jogador.sendMessage(ChatColor.RED + "Você não pode mexer no inventário enquanto digita uma senha!");
        }
    }

    private boolean estaDigitandoSenha(Player jogador) {
        return senhasDigitadas.containsKey(jogador) || inventariosSalvos.containsKey(jogador);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoColetarItem(org.bukkit.event.player.PlayerPickupItemEvent evento) {
        Player jogador = evento.getPlayer();
        
        if (senhasDigitadas.containsKey(jogador) || inventariosSalvos.containsKey(jogador)) {
            evento.setCancelled(true);
            //jogador.sendMessage(ChatColor.RED + "Você não pode pegar itens enquanto digita uma senha!");
        }
    }
}
