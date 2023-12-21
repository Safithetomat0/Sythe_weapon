package org.safi.weapons.scythe_weapon.networking;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public class Packets {
    public record vec3d(double x, double y, double z) {
        public PacketByteBuf write() {
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);

            return buffer;
        }

        public static vec3d read(PacketByteBuf buffer) {
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();

            return new vec3d(x,y,z);
        }
    }

}
