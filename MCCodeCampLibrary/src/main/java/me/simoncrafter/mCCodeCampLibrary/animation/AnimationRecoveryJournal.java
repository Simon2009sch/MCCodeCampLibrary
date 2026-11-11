package me.simoncrafter.mCCodeCampLibrary.animation;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Durable intent overrides potentially stale chunk PDC after a crash. Atomic moves are mandatory. */
final class AnimationRecoveryJournal {
    enum State { ACTIVE, RESTORING, EDITING, RETIRED, DELETED }
    record Entry(State state,Set<Long> pending) {
        Entry { pending=Set.copyOf(pending); }
    }
    private final Path directory;
    AnimationRecoveryJournal(Path directory)throws IOException {this.directory=directory;Files.createDirectories(directory);}
    Entry read(UUID id)throws IOException {
        Path file=directory.resolve(id+".recovery");
        if(!Files.exists(file))return null;
        try {
            List<String> lines=Files.readAllLines(file);
            if(lines.isEmpty())throw new IllegalArgumentException();
            State state=State.valueOf(lines.getFirst().toUpperCase(Locale.ROOT));
            Set<Long> pending=new HashSet<>();
            for(String line:lines.subList(1,lines.size()))if(!pending.add(Long.parseLong(line)))throw new IllegalArgumentException();
            if(state!=State.RESTORING && state!=State.RETIRED && !pending.isEmpty())throw new IllegalArgumentException();
            return new Entry(state,pending);
        }catch(IllegalArgumentException e){throw new IOException("Invalid recovery intent for "+id,e);}
    }
    void write(UUID id,Entry entry)throws IOException {
        Path temp=Files.createTempFile(directory,id+"-",".tmp");
        try {
            List<String> lines=new ArrayList<>();lines.add(entry.state().name().toLowerCase(Locale.ROOT));
            entry.pending().stream().sorted().map(String::valueOf).forEach(lines::add);
            Files.write(temp,lines);
            try(FileChannel channel=FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}
            Files.move(temp,directory.resolve(id+".recovery"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
}
