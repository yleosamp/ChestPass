# ChestPass 🔒

## Sobre
ChestPass é um plugin de proteção de baús para servidores Minecraft que resolve um problema comum em servidores SMP e Survival: o roubo de itens. Com ele, os jogadores podem proteger seus baús com senhas numéricas de 4 dígitos, garantindo que apenas pessoas autorizadas tenham acesso aos seus itens.

## 🌟 Características
- Sistema de senha numérica de 4 dígitos
- Interface visual intuitiva com cabeças coloridas
- Suporte a múltiplos idiomas (Inglês e Português)
- Sistema anti-roubo durante digitação da senha
- Proteção contra perda de itens
- Persistência de dados (senhas salvas mesmo após restart)
- Sistema de permissões integrado

## 📋 Como Usar

### Para Jogadores
1. **Proteger um Baú:**
   - Agache (Shift) + Clique com botão direito no baú
   - Digite uma senha de 4 dígitos usando as cabeças coloridas
   - Pronto! Seu baú está protegido

2. **Acessar um Baú Protegido:**
   - Clique com botão direito no baú
   - Digite a senha correta
   - O baú abrirá automaticamente

3. **Remover Senha:**
   - Apenas o dono pode remover
   - Agache (Shift) + Clique com botão direito no baú
   - A senha será removida automaticamente

### Para Administradores
- Configure o idioma em `messages.yml`
- Gerencie permissões:
  - `chestpass.use.on`: Permite usar o plugin (padrão: true)
  - `chestpass.use.off`: Bloqueia uso do plugin (padrão: false)

## ⚙️ Configuração
O plugin possui dois arquivos de configuração:

### messages.yml
language: en # ou pt-br, pt, br para Português

### data.yml
- Armazena dados dos baús protegidos
- Salvo automaticamente a cada 5 minutos
- Backup ao desligar servidor

## 🔒 Segurança
- Inventário bloqueado durante digitação da senha
- Proteção contra perda de itens
- Sistema anti-exploit
- Dados persistentes e seguros

## 📝 Permissões
- `chestpass.use.on`: Permite usar o plugin
- `chestpass.use.off`: Apenas permite abrir baús com senha

## 💡 Dicas
- O dono do baú não precisa digitar senha
- Senhas são numéricas de 4 dígitos
- Interface colorida para fácil memorização
- Sistema seguro contra perda de itens

## ⚠️ Requisitos
- Spigot/Paper 1.21.1+
- Java 21+
- Permissões básicas configuradas

## 🤝 Contribuindo
Sinta-se à vontade para contribuir com o projeto através de:
- Reporte de bugs
- Sugestões de melhorias
- Pull requests

## 📄 Licença
Este projeto está sob a licença MIT. Veja o arquivo LICENSE para mais detalhes.

