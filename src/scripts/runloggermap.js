const fs = require('fs');

const file = process.argv[2];
if (!file) throw 'no file';

const logs = JSON.parse(fs.readFileSync(file));
const roomData = JSON.parse(fs.readFileSync('../main/resources/assets/devonian/dungeons/rooms.json')).reduce((a, v) => {
  a[v.roomID] = v;
  return a;
}, []);

const floorFilter = new Set([]);

const floors = [
  [4, 4],
  [4, 5],
  [5, 5],
  [5, 5],
  [6, 5],
  [6, 6],
  [6, 6],
  [6, 6],
];
const roomTypes = {
  normal: 'O',
  yellow: 'Y',
  fairy: 'F',
  puzzle: 'P',
  entrance: 'E',
  trap: 'T',
  blood: 'B',
  rare: 'R',
};

Object.values(logs).forEach(o => {
  Object.entries(o).forEach(([floor, runs]) => {
    const floorNum = floor === 'E' ? 0 : +floor[1];
    if (floorFilter.size > 0 && !floorFilter.has(floorNum)) return;
    const size = floors[floorNum];

    runs.forEach(r => {
      process.stdout.write(floor + '\n');

      const [_, __, roomsS, doorsS] = r.currentDungeon.split(';');
      const rooms = roomsS.match(/(.{3})/g).map(v => roomData[+v]);
      const doors = doorsS.split('').map(v => v !== '9');

      for (let y = 0; y < size[1]; y++) {
        for (let x = 0; x < size[0]; x++) {
          const r = rooms[y * 6 + x] || {};
          const d = doors[y * 11 + x];
          const c = roomTypes[r.type] || ' ';
          process.stdout.write(c);
          if (x < size[0] - 1) {
            if (rooms[y * 6 + x + 1] === r) process.stdout.write(c);
            else process.stdout.write(d ? '-' : ' ');
          }
        }
        process.stdout.write('\n');
        if (y < size[1] - 1) {
          for (let x = 0; x < size[0]; x++) {
            const r = rooms[y * 6 + x] || {};
            const d = doors[y * 11 + x + 5];
            process.stdout.write(r === rooms[y * 6 + x + 6] ? roomTypes[r.type] || '?' : d ? '|' : ' ');
            if (x < size[0] - 1) {
              process.stdout.write(' ');
            }
          }
        }
        process.stdout.write('\n');
      }
      process.stdout.write('\n');
    });
  });
});