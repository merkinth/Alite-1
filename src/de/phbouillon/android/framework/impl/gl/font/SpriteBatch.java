package de.phbouillon.android.framework.impl.gl.font;

/* Alite - Discover the Universe on your Favorite Android Device
 * Copyright (C) 2015 Philipp Bouillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful and
 * fun, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see
 * http://http://www.gnu.org/licenses/gpl-3.0.txt.
 */

import android.opengl.GLES11;

class SpriteBatch {

   //--Constants--//
   private final static int VERTEX_SIZE = 4;                  // Vertex Size (in Components) ie. (X,Y,U,V)
   private final static int VERTICES_PER_SPRITE = 4;          // Vertices Per Sprite
   private final static int INDICES_PER_SPRITE = 6;           // Indices Per Sprite

   //--Members--//
   private Vertices vertices;                                 // Vertices Instance Used for Rendering
   private float[] vertexBuffer;                              // Vertex Buffer
   private int bufferIndex;                                   // Vertex Buffer Start Index
   private int maxSprites;                                    // Maximum Sprites Allowed in Buffer
   private int numSprites;                                    // Number of Sprites Currently in Buffer

   //--Constructor--//
   // D: prepare the sprite batcher for specified maximum number of sprites
   // A: gl - the gl instance to use for rendering
   //    maxSprites - the maximum allowed sprites per batch
   SpriteBatch(int maxSprites)  {
      vertexBuffer = new float[maxSprites * VERTICES_PER_SPRITE * VERTEX_SIZE];  // Create Vertex Buffer
      vertices = new Vertices(maxSprites * VERTICES_PER_SPRITE, maxSprites * INDICES_PER_SPRITE, false, true, false );  // Create Rendering Vertices
      bufferIndex = 0;                           // Reset Buffer Index
      this.maxSprites = maxSprites;                   // Save Maximum Sprites
      numSprites = 0;                            // Clear Sprite Counter

      short[] indices = new short[maxSprites * INDICES_PER_SPRITE];  // Create Temp Index Buffer
      int len = indices.length;                       // Get Index Buffer Length
      short j = 0;                                    // Counter
      for ( int i = 0; i < len; i+= INDICES_PER_SPRITE, j += VERTICES_PER_SPRITE )  {  // FOR Each Index Set (Per Sprite)
         indices[i + 0] = (short)( j + 0 );           // Calculate Index 0
         indices[i + 1] = (short)( j + 1 );           // Calculate Index 1
         indices[i + 2] = (short)( j + 2 );           // Calculate Index 2
         indices[i + 3] = (short)( j + 2 );           // Calculate Index 3
         indices[i + 4] = (short)( j + 3 );           // Calculate Index 4
         indices[i + 5] = (short)( j + 0 );           // Calculate Index 5
      }
      vertices.setIndices( indices, 0, len );         // Set Index Buffer for Rendering
   }

   //--Begin Batch--//
   // D: signal the start of a batch. set the texture and clear buffer
   //    NOTE: the overloaded (non-texture) version assumes that the texture is already bound!
   // A: textureId - the ID of the texture to use for the batch
   // R: [none]
   void beginBatch(int textureId)  {
	   GLES11.glBindTexture(GLES11.GL_TEXTURE_2D, textureId); // Bind Texture
      numSprites = 0;                                 // Empty Sprite Counter
      bufferIndex = 0;                                // Reset Buffer Index (Empty)
   }

   void beginBatch()  {
      numSprites = 0;                                 // Empty Sprite Counter
      bufferIndex = 0;                                // Reset Buffer Index (Empty)
   }

   //--End Batch--//
   // D: signal the end of a batch. render the batched sprites
   // A: [none]
   // R: [none]
   void endBatch()  {
      if ( numSprites > 0 )  {                        // IF Any Sprites to Render
         vertices.setVertices( vertexBuffer, 0, bufferIndex );  // Set Vertices from Buffer
         vertices.bind();
         vertices.draw( GLES11.GL_TRIANGLES, 0, numSprites * INDICES_PER_SPRITE );  // Render Batched Sprites
         vertices.unbind();                           // Unbind Vertices
      }
   }

   //--Draw Sprite to Batch--//
   // D: batch specified sprite to batch. adds vertices for sprite to vertex buffer
   //    NOTE: MUST be called after beginBatch(), and before endBatch()!
   //    NOTE: if the batch overflows, this will render the current batch, restart it,
   //          and then batch this sprite.
   // A: x, y - the x,y position of the sprite (center)
   //    width, height - the width and height of the sprite
   //    region - the texture region to use for sprite
   // R: [none]
   void drawSprite(float x, float y, float width, float height, CharacterData data)  {
      if ( numSprites == maxSprites )  {              // IF Sprite Buffer is Full
         endBatch();                                  // End Batch
         // NOTE: leave current texture bound!!
         numSprites = 0;                              // Empty Sprite Counter
         bufferIndex = 0;                             // Reset Buffer Index (Empty)
      }

      float halfWidth = width / 2.0f;                 // Calculate Half Width
      float halfHeight = height / 2.0f;               // Calculate Half Height
      float x1 = x - halfWidth;                       // Calculate Left X
      float y1 = y - halfHeight;                      // Calculate Bottom Y
      float x2 = x + halfWidth;                       // Calculate Right X
      float y2 = y + halfHeight;                      // Calculate Top Y

      vertexBuffer[bufferIndex++] = x1;               // Add X for Vertex 0
      vertexBuffer[bufferIndex++] = y1;               // Add Y for Vertex 0
      vertexBuffer[bufferIndex++] = data.uStart;      // Add U for Vertex 0
      vertexBuffer[bufferIndex++] = data.vStart;      // Add V for Vertex 0

      vertexBuffer[bufferIndex++] = x2;               // Add X for Vertex 1
      vertexBuffer[bufferIndex++] = y1;               // Add Y for Vertex 1
      vertexBuffer[bufferIndex++] = data.uEnd;        // Add U for Vertex 1
      vertexBuffer[bufferIndex++] = data.vStart;      // Add V for Vertex 1

      vertexBuffer[bufferIndex++] = x2;               // Add X for Vertex 2
      vertexBuffer[bufferIndex++] = y2;               // Add Y for Vertex 2
      vertexBuffer[bufferIndex++] = data.uEnd;        // Add U for Vertex 2
      vertexBuffer[bufferIndex++] = data.vEnd;        // Add V for Vertex 2

      vertexBuffer[bufferIndex++] = x1;               // Add X for Vertex 3
      vertexBuffer[bufferIndex++] = y2;               // Add Y for Vertex 3
      vertexBuffer[bufferIndex++] = data.uStart;      // Add U for Vertex 3
      vertexBuffer[bufferIndex++] = data.vEnd;        // Add V for Vertex 3

      numSprites++;                                   // Increment Sprite Count
   }
}
