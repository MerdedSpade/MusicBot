/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.music;

import java.util.List;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.MusicCommand;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlaylistsCmd extends MusicCommand 
{
    public PlaylistsCmd(Bot bot)
    {
        super(bot);
        this.name = "playlists";
        this.help = "показывает список доступных плэйлистов";
        this.aliases = new String[]{"pls"};
        this.guildOnly = true;
        this.beListening = false;
        this.beListening = false;
    }
    
    @Override
    public void doCommand(CommandEvent event) 
    {
        if(!bot.getPlaylistLoader().folderExists())
            bot.getPlaylistLoader().createFolder();
        if(!bot.getPlaylistLoader().folderExists())
        {
            event.reply(event.getClient().getWarning()+" Папка с плэйлистами не существует или не была создана!");
            return;
        }
        List<String> list = bot.getPlaylistLoader().getPlaylistNames();
        if(list==null)
            event.reply(event.getClient().getError()+" Не удалось загрузить доступные плэйлисты!");
        else if(list.isEmpty())
            event.reply(event.getClient().getWarning()+" Нет плэйлистов в папке плэйлистов!");
        else
        {
            StringBuilder builder = new StringBuilder(event.getClient().getSuccess()+" Доступные плэйлисты:\n");
            list.forEach(str -> builder.append("`").append(str).append("` "));
            builder.append("\nНапишите `").append(event.getClient().getTextualPrefix()).append("play playlist <имя>` чтобы воспроизвести плэйлист");
            event.reply(builder.toString());
        }
    }
}
