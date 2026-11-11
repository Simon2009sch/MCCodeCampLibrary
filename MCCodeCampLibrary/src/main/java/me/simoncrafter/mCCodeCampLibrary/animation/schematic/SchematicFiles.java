package me.simoncrafter.mCCodeCampLibrary.animation.schematic;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** The checksummed manifest is the commit record; recapture atomically switches to a new generation. */
final class SchematicFiles {
    record Payload(byte[] schematic, byte[] metadata) {}
    private final Path directory;
    SchematicFiles(Path directory) throws IOException { this.directory=directory; Files.createDirectories(directory); }
    synchronized void commit(UUID id, byte[] schematic, byte[] metadata) throws IOException {
        Path target=directory.resolve(id+".schem"), manifest=directory.resolve(id+".snapshot");
        if(Files.exists(target)||Files.exists(manifest))throw new IOException("Snapshot already exists (including incomplete capture): "+id);
        Path part=Files.createTempFile(directory,id+"-",".part"), meta=Files.createTempFile(directory,id+"-",".part");
        try {
            Files.write(part,schematic); force(part);
            try(var out=new DataOutputStream(Files.newOutputStream(meta))) {
                out.writeInt(1); out.writeUTF(hash(schematic)); out.writeInt(metadata.length); out.write(metadata); out.writeUTF(hash(metadata));
            }
            force(meta);
            Files.move(part,target,StandardCopyOption.ATOMIC_MOVE);
            Files.move(meta,manifest,StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(part);Files.deleteIfExists(meta); }
    }
    synchronized void replace(UUID id,byte[] schematic,byte[] metadata)throws IOException {
        read(id); // Refuse to silently overwrite a corrupt original.
        String generation=id+"-"+UUID.randomUUID()+".schem";
        Path target=directory.resolve(generation), meta=Files.createTempFile(directory,id+"-",".part");
        try {
            Files.write(target,schematic,StandardOpenOption.CREATE_NEW);force(target);
            try(var out=new DataOutputStream(Files.newOutputStream(meta))) {
                out.writeInt(2);out.writeUTF(generation);out.writeUTF(hash(schematic));
                out.writeInt(metadata.length);out.write(metadata);out.writeUTF(hash(metadata));
            }
            force(meta);
            Files.move(meta,directory.resolve(id+".snapshot"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(meta);}
    }
    synchronized Payload read(UUID id) throws IOException {
        try(var in=new DataInputStream(Files.newInputStream(directory.resolve(id+".snapshot")))) {
            int version=in.readInt();String name;
            if(version==1)name=id+".schem";
            else if(version==2) {
                name=in.readUTF();
                if(!name.matches(java.util.regex.Pattern.quote(id.toString())+"-[0-9a-f-]{36}\\.schem"))throw new IOException("Invalid generation filename");
            }else throw new IOException("Invalid schematic manifest version");
            byte[] schematic=Files.readAllBytes(directory.resolve(name));
            if(!in.readUTF().equals(hash(schematic)))throw new IOException("Invalid schematic checksum");
            int n=in.readInt(); if(n<0||n>16_000_000)throw new IOException("Invalid selection metadata length");
            byte[] metadata=in.readNBytes(n);
            if(metadata.length!=n||!in.readUTF().equals(hash(metadata))||in.read()!=-1)throw new IOException("Invalid selection metadata");
            return new Payload(schematic,metadata);
        }
    }
    private static void force(Path path)throws IOException {try(var channel=FileChannel.open(path,StandardOpenOption.WRITE)){channel.force(true);}}
    private static String hash(byte[] bytes) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
}
