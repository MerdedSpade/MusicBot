/*
 * Copyright 2017 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.settings.Settings;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SettingsCmd extends Command 
{
    public SettingsCmd()
    {
        this.name = "settings";
        this.help = "показывает настройки бота";
        this.aliases = new String[]{"sets"};
        this.guildOnly = true;
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        MessageBuilder builder = new MessageBuilder()
                .append("\uD83C\uDFA7 **")
                .append(event.getSelfUser().getName())
                .append("** Настройки:");
        TextChannel tchan = s.getTextChannel(event.getGuild());
        VoiceChannel vchan = s.getVoiceChannel(event.getGuild());
        Role role = s.getRole(event.getGuild());
        EmbedBuilder ebuilder = new EmbedBuilder()
                .setColor(event.getSelfMember().getColor())
                .setDescription("Текстовый канал: "+(tchan==null ? "Любой" : "**#"+tchan.getName()+"**")
                        + "\nГолосовой канал: "+(vchan==null ? "Любой" : "**"+vchan.getName()+"**")
                        + "\nDJ роль: "+(role==null ? "Нету" : "**"+role.getName()+"**")
                        + "\nРежим повтора: **"+(s.getRepeatMode() ? "On" : "Off")+"**"
                        + "\nСтандартный плэйлист: "+(s.getDefaultPlaylist()==null ? "Нету" : "**"+s.getDefaultPlaylist()+"**")
                        )
                .setFooter(event.getJDA().getGuilds().size()+" серверов | "
                        +event.getJDA().getGuilds().stream().filter(g -> g.getSelfMember().getVoiceState().inVoiceChannel()).count()
                        +" audio connections", null);
        event.getChannel().sendMessage(builder.setEmbed(ebuilder.build()).build()).queue();
    }
    
}
