package net.runelite.injector.raw;

import net.runelite.asm.Method;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.Label;
import net.runelite.asm.attributes.code.instruction.types.ComparisonInstruction;
import net.runelite.asm.attributes.code.instruction.types.JumpingInstruction;
import net.runelite.asm.attributes.code.instructions.*;
import net.runelite.asm.pool.Field;
import net.runelite.injector.Inject;
import net.runelite.injector.InjectUtil;
import net.runelite.injector.InjectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ListIterator;

public class HidePlayerAttacks
{
	private final Logger log = LoggerFactory.getLogger(HidePlayerAttacks.class);
	private final Inject inject;

	public HidePlayerAttacks(Inject inject)
	{
		this.inject = inject;
	}

	public void inject() throws InjectionException
	{
		final Method addPlayerOptions = InjectUtil.findMethod(inject, "addPlayerToMenu");
		final net.runelite.asm.pool.Method shouldHideAttackOptionFor = inject.getVanilla().findClass("client")
				.findMethod("shouldHideAttackOptionFor").getPoolMethod();

		try
		{
			injectHideAttack(addPlayerOptions, shouldHideAttackOptionFor);
			injectHideCast(addPlayerOptions, shouldHideAttackOptionFor);
		}
		catch (InjectionException | AssertionError e)
		{
			log.warn("HidePlayerAttacks injection failed, but as this does not interfere with other functionality we will continue", e);
		}
	}

	private void injectHideAttack(Method addPlayerOptions, net.runelite.asm.pool.Method shouldHideAttackOptionFor) throws InjectionException
	{
		final Field AttackOption_hidden = InjectUtil.findDeobField(inject, "AttackOption_hidden", "AttackOption").getPoolField();
		final Field attackOption = InjectUtil.findDeobField(inject, "playerAttackOption", "Client").getPoolField();

		// GETSTATIC					GETSTATIC
		// GETSTATIC					GETSTATIC
		// IFACMPEQ -> label continue	IFACMPNE -> label whatever lets carry on
		//								MORE OBFUSCATION

		int injectIdx = -1;
		Instruction labelIns = null;
		Label label = null;

		Instructions ins = addPlayerOptions.getCode().getInstructions();
		Iterator<Instruction> iterator = ins.getInstructions().iterator();
		while (iterator.hasNext())
		{
			Instruction i = iterator.next();
			if (!(i instanceof GetStatic))
			{
				continue;
			}

			Field field = ((GetStatic) i).getField();
			if (!field.equals(AttackOption_hidden) && !field.equals(attackOption))
			{
				continue;
			}

			i = iterator.next();
			if (!(i instanceof GetStatic))
			{
				continue;
			}

			field = ((GetStatic) i).getField();
			if (!field.equals(AttackOption_hidden) && !field.equals(attackOption))
			{
				continue;
			}

			i = iterator.next();
			if (!(i instanceof ComparisonInstruction && i instanceof JumpingInstruction))
			{
				log.warn("This if block should not be triggered. There might be something wrong with the HidePlayerAttacks injection");
				continue;
			}

			if (i instanceof IfACmpEq)
			{
				injectIdx = ins.getInstructions().indexOf(i) + 1;
				label = ((IfACmpEq) i).getJumps().get(0);
			}
			else if (i instanceof IfACmpNe)
			{
				injectIdx = ins.getInstructions().indexOf(((IfACmpNe) i).getJumps().get(0)) + 1;
				// We're gonna have to inject a extra label
				labelIns = iterator.next();
			}

			break;
		}

		if (injectIdx <= 0 || label == null && labelIns == null)
		{
			throw new InjectionException("Injecting HidePlayerAttacks failed");
		}

		// Load the player
		ALoad i1 = new ALoad(ins, 0);
		// Get the boolean
		InvokeStatic i2 = new InvokeStatic(ins, shouldHideAttackOptionFor);

		ins.addInstruction(injectIdx, i1);
		ins.addInstruction(injectIdx + 1, i2);

		if (label == null)
		{
			label = ins.createLabelFor(labelIns);
			ins.rebuildLabels();
			injectIdx = ins.getInstructions().indexOf(i2) + 1;
		}

		IfNe i3 = new IfNe(ins, label);

		ins.addInstruction(injectIdx, i3);
	}

	private void injectHideCast(Method addPlayerOptions, net.runelite.asm.pool.Method shouldHideAttackOptionFor) throws InjectionException
	{
		// LABEL before
		// BIPUSH 8
		// LDC (garbage)
		// GETSTATIC selectedSpellFlags
		// IMUL
		// BIPUSH 8
		// IAND
		// IF_ICMPNE -> skip adding option
		//
		// <--- Inject call here
		// <--- Inject comparison here
		//
		// add option
		final Field flags = InjectUtil.findDeobField(inject, "selectedSpellFlags", "Client").getPoolField();
		Instructions ins = addPlayerOptions.getCode().getInstructions();
		ListIterator<Instruction> iterator = ins.getInstructions().listIterator();
		boolean b1, b2, iAnd, getStatic;
		b1 = b2 = iAnd = getStatic = false;
		while (iterator.hasNext())
		{
			Instruction i = iterator.next();

			if (i instanceof Label)
			{
				b1 = b2 = iAnd = getStatic = false;
				continue;
			}

			if ((i instanceof BiPush) && (byte) ((BiPush) i).getConstant() == 8)
			{
				if (!b1)
					b1 = true;
				else if (!b2)
					b2 = true;
				else
					throw new InjectionException("Error injecting HideCastOptions in HidePlayerAttacks: more than 2 BiPushes");

				continue;
			}

			if (i instanceof IAnd)
			{
				iAnd = true;
				continue;
			}

			if (i instanceof GetStatic && ((GetStatic) i).getField().equals(flags))
			{
				getStatic = true;
				continue;
			}


			if (!(i instanceof JumpingInstruction))
			{
				if (b1 && b2 && iAnd && getStatic)
				{
					throw new InjectionException("Error injecting HideCastOptions in HidePlayerAttacks");
				}
				continue;
			}

			if (!(b1 && b2 && iAnd && getStatic))
			{
				continue;
			}

			Label target;
			if (i instanceof IfICmpNe)
			{
				target = ((IfICmpNe) i).getJumps().get(0);
			}
			else
			{
				throw new InjectionException("Error injecting HideCastOptions in HidePlayerAttacks");
			}

			// Load the player
			ALoad i1 = new ALoad(ins, 0);
			// Get the boolean
			InvokeStatic i2 = new InvokeStatic(ins, shouldHideAttackOptionFor);
			// Compare
			IfNe i3 = new IfNe(ins, target);

			iterator.add(i1);
			iterator.add(i2);
			iterator.add(i3);
			return;
		}
	}
}
