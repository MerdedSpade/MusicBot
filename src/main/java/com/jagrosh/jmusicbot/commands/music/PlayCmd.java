/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.exceptions.PermissionException;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlayCmd extends MusicCommand
{
    public final static String LOAD = "\uD83D\uDCE5";
    public final static String CANCEL = "\uD83D\uDEAB";
    
    private final String loadingEmoji;
    
    public PlayCmd(Bot bot, String loadingEmoji)
    {
        super(bot);
        this.loadingEmoji = loadingEmoji;
        this.name = "play";
        this.arguments = "<название|URL|подкоманда>";
        this.help = "показывает предоставленную песн.";
        this.beListening = true;
        this.bePlaying = false;
        this.children = new Command[]{new PlaylistCmd(bot)};
    }

    @Override
    public void doCommand(CommandEvent event) 
    {
        if(event.getArgs().isEmpty() && event.getMessage().getAttachments().isEmpty())
        {
            AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
            if(handler.getPlayer().getPlayingTrack()!=null && handler.getPlayer().isPaused())
            {
                boolean isDJ = event.getMember().hasPermission(Permission.MANAGE_SERVER);
                if(!isDJ)
                    isDJ = event.isOwner();
                Settings settings = event.getClient().getSettingsFor(event.getGuild());
                Role dj = settings.getRole(event.getGuild());
                if(!isDJ && dj!=null)
                    isDJ = event.getMember().getRoles().contains(dj);
                if(!isDJ)
                    event.replyError("Только DJи могут возобновить проигрователь!");
                else
                {
                    handler.getPlayer().setPaused(false);
                    event.replySuccess("Возоблено **"+handler.getPlayer().getPlayingTrack().getInfo().title+"**.");
                }
                return;
            }
            StringBuilder builder = new StringBuilder(event.getClient().getWarning()+" Команды Воспроизведения:\n");
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" <название песни>` - воспроизводит первый результат из YouTube");
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" <URL>` - играет предоставленную песню, лист воспроизведения, или трансляцию");
            for(Command cmd: children)
                builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName()).append(" ").append(cmd.getArguments()).append("` - ").append(cmd.getHelp());
            event.reply(builder.toString());
            return;
        }
        String args = event.getArgs().startsWith("<") && event.getArgs().endsWith(">") 
                ? event.getArgs().substring(1,event.getArgs().length()-1) 
                : event.getArgs().isEmpty() ? event.getMessage().getAttachments().get(0).getUrl() : event.getArgs();
        event.reply(loadingEmoji+" Загрузка... `["+args+"]`", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), args, new ResultHandler(m,event,false)));
    }
    
    private class ResultHandler implements AudioLoadResultHandler
    {
        private final Message m;
        private final CommandEvent event;
        private final boolean ytsearch;
        
        private ResultHandler(Message m, CommandEvent event, boolean ytsearch)
        {
            this.m = m;
            this.event = event;
            this.ytsearch = ytsearch;
        }
        
        private void loadSingle(AudioTrack track, AudioPlaylist playlist)
        {
            if(bot.getConfig().isTooLong(track))
            {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" This track (**"+track.getInfo().title+"**) is longer than the allowed maximum: `"
                        +FormatUtil.formatTime(track.getDuration())+"` > `"+FormatUtil.formatTime(bot.getConfig().getMaxSeconds()*1000)+"`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor()))+1;
            String addMsg = FormatUtil.filter(event.getClient().getSuccess()+" Добавлено **"+track.getInfo().title
                    +"** (`"+FormatUtil.formatTime(track.getDuration())+"`) "+(pos==0?"в начало воспроизведения":" в очередь на позицию "+pos));
            if(playlist==null || !event.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_ADD_REACTION))
                m.editMessage(addMsg).queue();
            else
            {
                new ButtonMenu.Builder()
                        .setText(addMsg+"\n"+event.getClient().getWarning()+" Этот трэк имеет плэйлист **"+playlist.getTracks().size()+"** трэков добавлено. Выберите "+LOAD+" для загрузки плэйлиста.")
                        .setChoices(LOAD, CANCEL)
                        .setEventWaiter(bot.getWaiter())
                        .setTimeout(30, TimeUnit.SECONDS)
                        .setAction(re ->
                        {
                            if(re.getName().equals(LOAD))
                                m.editMessage(addMsg+"\n"+event.getClient().getSuccess()+" Loaded **"+loadPlaylist(playlist, track)+"** additional tracks!").queue();
                            else
                                m.editMessage(addMsg).queue();
                        }).setFinalAction(m ->
                        {
                            try{m.clearReactions().queue();}catch(PermissionException ex){}
                        }).build().display(m);
            }
        }
        
        private int loadPlaylist(AudioPlaylist playlist, AudioTrack exclude)
        {
            int[] count = {0};
            playlist.getTracks().stream().forEach((track) -> {
                if(!bot.getConfig().isTooLong(track) && !track.equals(exclude))
                {
                    AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
                    handler.addTrack(new QueuedTrack(track, event.getAuthor()));
                    count[0]++;
                }
            });
            return count[0];
        }
        
        @Override
        public void trackLoaded(AudioTrack track)
        {
            loadSingle(track, null);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist)
        {
            if(playlist.getTracks().size()==1 || playlist.isSearchResult())
            {
                AudioTrack single = playlist.getSelectedTrack()==null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                loadSingle(single, null);
            }
            else if (playlist.getSelectedTrack()!=null)
            {
                AudioTrack single = playlist.getSelectedTrack();
                loadSingle(single, playlist);
            }
            else
            {
                int count = loadPlaylist(playlist, null);
                if(count==0)
                {
                    m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" Все записи в плэйлисте "+(playlist.getName()==null ? "" : "(**"+playlist.getName()
                            +"**) ")+"больше разрешённого максимума (`"+bot.getConfig().getMaxTime()+"`)")).queue();
                }
                else
                {
                    m.editMessage(FormatUtil.filter(event.getClient().getSuccess()+" Найдено "
                            +(playlist.getName()==null?"плэйлист":"плэйлист **"+playlist.getName()+"**")+" с `"
                            + playlist.getTracks().size()+"` пунктов; добавлено в очередь!"
                            + (count<playlist.getTracks().size() ? "\n"+event.getClient().getWarning()+" Трэки длиннее разрешённого максимума (`"
                            + bot.getConfig().getMaxTime()+"`) были пропущены." : ""))).queue();
                }
            }
        }

        @Override
        public void noMatches()
        {
            if(ytsearch)
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" Не найдено результатов для `"+event.getArgs()+"`.")).queue();
            else
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:"+event.getArgs(), new ResultHandler(m,event,true));
        }

        @Override
        public void loadFailed(FriendlyException throwable)
        {
            if(throwable.severity==Severity.COMMON)
                m.editMessage(event.getClient().getError()+" Ошибка загрузки: "+throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError()+" Ошибка загрузки трэка.").queue();
        }
    }
    
    public class PlaylistCmd extends MusicCommand
    {
        public PlaylistCmd(Bot bot)
        {
            super(bot);
            this.name = "playlist";
            this.aliases = new String[]{"pl"};
            this.arguments = "<имя>";
            this.help = "воспроизводит предоставленные плэйлисты";
            this.beListening = true;
            this.bePlaying = false;
        }

        @Override
        public void doCommand(CommandEvent event) 
        {
            if(event.getArgs().isEmpty())
            {
                event.reply(event.getClient().getError()+" Пожалуйста добавьте имя плэйлиста.");
                return;
            }
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(event.getArgs());
            if(playlist==null)
            {
                event.replyError("Я не могу найти `"+event.getArgs()+".txt` в папке с плэйлистами.");
                return;
            }
            event.getChannel().sendMessage(loadingEmoji+" Загрузка плэйлиста **"+event.getArgs()+"**... ("+playlist.getItems().size()+" items)").queue(m -> 
            {
                AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at)->handler.addTrack(new QueuedTrack(at, event.getAuthor())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty() 
                            ? event.getClient().getWarning()+" Трэки не были загружены!" 
                            : event.getClient().getSuccess()+" Загружено **"+playlist.getTracks().size()+"** трэков!");
                    if(!playlist.getErrors().isEmpty())
                        builder.append("\nСледующие трэки не удалось загрузить:");
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex()+1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if(str.length()>2000)
                        str = str.substring(0,1994)+" (...)";
                    m.editMessage(FormatUtil.filter(str)).queue();
                });
            });
        }
    }
}
