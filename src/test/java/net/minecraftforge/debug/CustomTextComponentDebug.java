package net.minecraftforge.debug;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.text.TextComponentItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid="CustomTextComponentDebug", name="CustomTextComponentDebug", version="0.0.0", acceptableRemoteVersions="*")
public class CustomTextComponentDebug
{
    // NOTE: Test with both this ON and OFF - ensure none of the test behaviours show when this is off!
    private static final boolean ENABLE = true;

    @EventHandler
    public void preinit(FMLPreInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void playerLogin(EntityJoinWorldEvent ev)
    {
        if(ENABLE && !ev.getWorld().isRemote && (ev.getEntity() instanceof EntityPlayer))
        {
            ItemStack stack = new ItemStack(Items.COOKIE);
            stack.setStackDisplayName("Jaffa Cake");

            ev.getEntity().addChatMessage(new TextComponentItemStack(stack));
        }
    }

}
