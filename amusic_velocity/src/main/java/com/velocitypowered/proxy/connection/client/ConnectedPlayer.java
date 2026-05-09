package com.velocitypowered.proxy.connection.client;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.network.HandshakeIntent;
import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.PluginMessageEncoder;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.GameProfile.Property;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.api.util.ServerLink;
import com.velocitypowered.proxy.connection.MinecraftConnection;

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public class ConnectedPlayer implements Player {
	
	public MinecraftConnection getConnection() {
	    return null;
	}

	@Override
	public Tristate getPermissionValue(String permission) {
		return null;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return null;
	}

	@Override
	public Optional<InetSocketAddress> getVirtualHost() {
		return Optional.empty();
	}

	@Override
	public Optional<String> getRawVirtualHost() {
		return Optional.empty();
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public ProtocolVersion getProtocolVersion() {
		return null;
	}

	@Override
	public ProtocolState getProtocolState() {
		return null;
	}

	@Override
	public HandshakeIntent getHandshakeIntent() {
		return null;
	}

	@Override
	public @NotNull Identity identity() {
		return null;
	}

	@Override
	public @Nullable IdentifiedKey getIdentifiedKey() {
		return null;
	}

	@Override
	public String getUsername() {
		return null;
	}

	@Override
	public @Nullable Locale getEffectiveLocale() {
		return null;
	}

	@Override
	public void setEffectiveLocale(Locale locale) {
		
	}

	@Override
	public UUID getUniqueId() {
		return null;
	}

	@Override
	public Optional<ServerConnection> getCurrentServer() {
		return Optional.empty();
	}

	@Override
	public PlayerSettings getPlayerSettings() {
		return null;
	}

	@Override
	public boolean hasSentPlayerSettings() {
		return false;
	}

	@Override
	public Optional<ModInfo> getModInfo() {
		return Optional.empty();
	}

	@Override
	public long getPing() {
		return 0;
	}

	@Override
	public boolean isOnlineMode() {
		return false;
	}

	@Override
	public ConnectionRequestBuilder createConnectionRequest(RegisteredServer server) {
		return null;
	}

	@Override
	public List<Property> getGameProfileProperties() {
		return null;
	}

	@Override
	public void setGameProfileProperties(List<Property> properties) {
	}

	@Override
	public GameProfile getGameProfile() {
		return null;
	}

	@Override
	public void clearPlayerListHeaderAndFooter() {
	}

	@Override
	public Component getPlayerListHeader() {
		return null;
	}

	@Override
	public Component getPlayerListFooter() {
		return null;
	}

	@Override
	public TabList getTabList() {
		return null;
	}

	@Override
	public void disconnect(Component reason) {
		
	}

	@Override
	public void spoofChatInput(String input) {
		
	}

	@Override
	public void sendResourcePack(String url) {
		
	}

	@Override
	public void sendResourcePack(String url, byte[] hash) {
		
	}

	@Override
	public void sendResourcePackOffer(ResourcePackInfo packInfo) {
		
	}

	@Override
	public @Nullable ResourcePackInfo getAppliedResourcePack() {
		return null;
	}

	@Override
	public @Nullable ResourcePackInfo getPendingResourcePack() {
		return null;
	}

	@Override
	public @NotNull Collection<ResourcePackInfo> getAppliedResourcePacks() {
		return null;
	}

	@Override
	public @NotNull Collection<ResourcePackInfo> getPendingResourcePacks() {
		return null;
	}

	@Override
	public boolean sendPluginMessage(@NotNull ChannelIdentifier identifier, byte @NotNull [] data) {
		return false;
	}

	@Override
	public boolean sendPluginMessage(@NotNull ChannelIdentifier identifier, @NotNull PluginMessageEncoder dataEncoder) {
		return false;
	}

	@Override
	public @Nullable String getClientBrand() {
		return null;
	}

	@Override
	public void addCustomChatCompletions(@NotNull Collection<String> completions) {
	}

	@Override
	public void removeCustomChatCompletions(@NotNull Collection<String> completions) {
	}

	@Override
	public void setCustomChatCompletions(@NotNull Collection<String> completions) {
	}

	@Override
	public void transferToHost(@NotNull InetSocketAddress address) {
	}

	@Override
	public void storeCookie(Key key, byte[] data) {
	}

	@Override
	public void requestCookie(Key key) {
	}

	@Override
	public void setServerLinks(@NotNull List<ServerLink> links) {
	}

}
