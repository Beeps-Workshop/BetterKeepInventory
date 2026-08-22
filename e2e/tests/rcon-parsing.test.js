import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { extractEntityData, nbtField } from '../src/rcon.js';

/**
 * The one part of the harness that is pure, and the part most likely to be quietly wrong.
 *
 * These are real replies captured from a 1.21.8 server. Note that the per-entity messages
 * are run together with no separator at all -- that is not a transcription slip, it is
 * what the server's RCON transport actually sends, and parsing it line by line silently
 * reports a single item.
 */
describe('rcon reply parsing', () => {
  it('splits a batched compound reply into one value per entity', () => {
    const reply = 'Golden Apple has the following entity data: {id: "minecraft:golden_apple", count: 1}'
      + 'Oak Log has the following entity data: {id: "minecraft:oak_log", count: 7}';

    const values = extractEntityData(reply);

    assert.equal(values.length, 2);
    assert.equal(nbtField(values[0], 'id'), '"minecraft:golden_apple"');
    assert.equal(nbtField(values[1], 'count'), '7');
  });

  it('splits a batched scalar reply, where values butt against the next entity name', () => {
    const reply = 'Experience Orb has the following entity data: 27s'
      + 'Experience Orb has the following entity data: 8s';

    assert.deepEqual(extractEntityData(reply), ['27s', '8s']);
  });

  it('handles a list payload', () => {
    const reply = 'BkiBot has the following entity data: [1000.5d, 101.0d, 1000.5d]';

    assert.deepEqual(extractEntityData(reply), ['[1000.5d, 101.0d, 1000.5d]']);
  });

  it('returns nothing for the empty reply a zero-match execute produces', () => {
    assert.deepEqual(extractEntityData(''), []);
  });

  it('reads top-level fields, not ones nested inside item components', () => {
    // A written book carries an `id` deep inside its components. A regex over the whole
    // blob picks that up and reports the wrong material.
    const blob = '{id: "minecraft:written_book", count: 1, components: '
      + '{"minecraft:custom_data": {id: "minecraft:diamond"}}}';

    assert.equal(nbtField(blob, 'id'), '"minecraft:written_book"');
    assert.equal(nbtField(blob, 'count'), '1');
  });

  it('is not confused by braces or commas inside quoted strings', () => {
    const blob = '{id: "minecraft:paper", count: 3, components: {"minecraft:custom_name": "{a, b}"}}';

    assert.equal(nbtField(blob, 'id'), '"minecraft:paper"');
    assert.equal(nbtField(blob, 'count'), '3');
  });

  it('reports a missing field as absent rather than guessing', () => {
    assert.equal(nbtField('{id: "minecraft:stone"}', 'count'), null);
  });
});
