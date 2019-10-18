package net.runelite.mixins;

import java.awt.*;

import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.mixins.Inject;
import net.runelite.api.mixins.Mixin;
import net.runelite.api.mixins.Shadow;
import net.runelite.rs.api.RSClient;
import net.runelite.rs.api.RSEntity;
import net.runelite.rs.api.RSModel;
import net.runelite.rs.api.RSWallDecoration;

@Mixin(RSWallDecoration.class)
public abstract class RSWallDecorationMixin implements RSWallDecoration
{
	@Shadow("client")
	private static RSClient client;

	@Inject
	private int decorativeObjectPlane;

	@Inject
	@Override
	public int getPlane()
	{
		return decorativeObjectPlane;
	}

	@Inject
	@Override
	public void setPlane(int plane)
	{
		this.decorativeObjectPlane = plane;
	}

	@Inject
	@Override
	public RSModel getModel1()
	{
		RSEntity entity = getEntity1();
		if (entity == null)
		{
			return null;
		}

		RSModel model;

		if (entity instanceof Model)
		{
			model = (RSModel) entity;
		}
		else
		{
			model = entity.getModel();
		}

		return model;
	}

	@Inject
	@Override
	public RSModel getModel2()
	{
		RSEntity entity = getEntity2();
		if (entity == null)
		{
			return null;
		}

		RSModel model;

		if (entity instanceof Model)
		{
			model = (RSModel) entity;
		}
		else
		{
			model = entity.getModel();
		}

		return model;
	}

	@Inject
	@Override
	public Shape getClickbox()
	{
		LocalPoint lp = getLocalLocation();
		Shape clickboxA = Perspective.getClickbox(client, getModel1(), 0,
			new LocalPoint(lp.getX() + getXOffset(), lp.getY() + getYOffset()));
		Shape clickboxB = Perspective.getClickbox(client, getModel2(), 0, lp);

		if (clickboxA == null && clickboxB == null)
		{
			return null;
		}

		if (clickboxA != null)
		{
			return clickboxA;
		}

		return clickboxB;
	}

	@Inject
	@Override
	public Polygon getConvexHull()
	{
		RSModel model = getModel1();

		if (model == null)
		{
			return null;
		}

		int tileHeight = Perspective.getTileHeight(client, new LocalPoint(getX(), getY()), client.getPlane());
		return model.getConvexHull(getX() + getXOffset(), getY() + getYOffset(), 0, tileHeight);
	}

	@Inject
	@Override
	public Polygon getConvexHull2()
	{
		RSModel model = getModel2();

		if (model == null)
		{
			return null;
		}

		int tileHeight = Perspective.getTileHeight(client, new LocalPoint(getX(), getY()), client.getPlane());
		return model.getConvexHull(getX(), getY(), 0, tileHeight);
	}
}