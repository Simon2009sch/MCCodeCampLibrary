package me.simoncrafter.mCCodeCampLibrary.animation;

import java.nio.file.*;
import java.util.*;

/** Actual disk journal and exact-cell ownership behavior, independent of Bukkit. */
public class AnimationRecoveryHarness {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);UUID a=UUID.randomUUID(),b=UUID.randomUUID(),world=UUID.randomUUID();
        AnimationRecoveryJournal journal=new AnimationRecoveryJournal(root);
        journal.write(a,new AnimationRecoveryJournal.Entry(AnimationRecoveryJournal.State.EDITING,Set.of()));
        var reread=new AnimationRecoveryJournal(root).read(a);
        if(reread.state()!=AnimationRecoveryJournal.State.EDITING)throw new AssertionError("Editing intent lost across reload");
        journal.write(a,new AnimationRecoveryJournal.Entry(AnimationRecoveryJournal.State.RESTORING,Set.of(1L,2L)));
        if(!journal.read(a).pending().equals(Set.of(1L,2L)))throw new AssertionError("Pending restore chunks lost");
        journal.write(a,new AnimationRecoveryJournal.Entry(AnimationRecoveryJournal.State.ACTIVE,Set.of()));
        if(journal.read(a).state()!=AnimationRecoveryJournal.State.ACTIVE)throw new AssertionError("Finish intent lost");
        AnimationSourceClaims claims=new AnimationSourceClaims();
        var cell=new AnimationSourceClaims.Cell(1,2,3);
        claims.replace(a,world,Set.of(cell));
        try{claims.check(b,world,Set.of(cell));throw new AssertionError("Overlapping capture allowed");}catch(IllegalArgumentException expected){}
        claims.check(b,UUID.randomUUID(),Set.of(cell));
        claims.replace(a,world,Set.of(new AnimationSourceClaims.Cell(4,5,6)));
        claims.check(b,world,Set.of(cell));
        claims.release(a);claims.check(b,world,Set.of(new AnimationSourceClaims.Cell(4,5,6)));
        Files.writeString(root.resolve(a+".recovery"),"broken");
        try{journal.read(a);throw new AssertionError("Corrupt recovery intent accepted");}catch(java.io.IOException expected){}
        System.out.println("AnimationRecoveryHarness passed: durable edit/active/pending intent, exact overlap ownership, corrupt-state rejection");
    }
}
