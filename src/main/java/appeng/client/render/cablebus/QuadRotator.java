/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.render.cablebus;


import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;

import appeng.client.render.FacingToRotation;
import appeng.thirdparty.codechicken.lib.model.CachedFormat;
import appeng.thirdparty.codechicken.lib.model.Quad;
import appeng.thirdparty.codechicken.lib.model.pipeline.BakedPipeline;
import appeng.thirdparty.codechicken.lib.model.pipeline.transformers.QuadMatrixTransformer;


/**
 * Assuming a default-orientation of forward=NORTH and up=UP, this class rotates a given list of quads to the desired
 * facing
 */
public class QuadRotator
{
	private static final ThreadLocal<BakedPipeline> pipelines = ThreadLocal.withInitial( () -> //
			BakedPipeline.builder()//
					.addElement( "transformer", QuadMatrixTransformer.FACTORY )//
					.build() );
	private static final ThreadLocal<Quad> collectors = ThreadLocal.withInitial( Quad::new );

	public List<BakedQuad> rotateQuads( List<BakedQuad> quads, Direction newForward, Direction newUp )
	{
		if( newForward == Direction.NORTH && newUp == Direction.UP )
		{
			return quads; // This is the default orientation
		}
		FacingToRotation rotation = getRotation( newForward, newUp );
		if( rotation.isRedundant() )
		{
			return quads;
		}

		List<BakedQuad> result = new ArrayList<>( quads.size() );

		CachedFormat format = CachedFormat.lookup( DefaultVertexFormats.BLOCK );
		BakedPipeline pipeline = pipelines.get();
		Quad collector = collectors.get();
		pipeline.reset( format );
		collector.reset( format );
		QuadMatrixTransformer transformer = pipeline.getElement( "transformer", QuadMatrixTransformer.class );

		for( BakedQuad quad : quads )
		{
			transformer.setMatrix( rotation.getMat() );
			pipeline.prepare( collector );
			quad.pipe( pipeline );
			result.add( collector.bake() );
		}

		return result;
	}

	private FacingToRotation getRotation( Direction forward, Direction up )
	{
		// Sanitize forward/up
		if( forward.getAxis() == up.getAxis() )
		{
			if( up.getAxis() == Direction.Axis.Y )
			{
				up = Direction.NORTH;
			}
			else
			{
				up = Direction.UP;
			}
		}

		return FacingToRotation.get( forward, up );
	}
}
