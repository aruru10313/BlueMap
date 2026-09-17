<template>
  <div v-if="blockManager && blockState" class="timelapse-container" :class="{'open': blockState.timelapseOpen}">
    <!-- Floating Collapsed Button (when closed) -->
    <button v-if="!blockState.timelapseOpen" class="timelapse-toggle-btn" @click="toggleOpen" title="타임랩스 / 실시간 블록 타임라인 열기">
      <span class="icon">⏱</span>
      <span class="label">시간순 타임랩스</span>
      <span v-if="blockState.renderedBlocks > 0" class="badge">
        {{ blockState.renderedBlocks }}
      </span>
      <span class="live-dot-mini" :class="{'pulsing': blockState.isPlaying || blockState.progress >= 0.99}"></span>
    </button>

    <!-- Main Expanded Control Panel -->
    <div v-else class="timelapse-panel">
      <!-- Top info bar -->
      <div class="panel-header">
        <div class="header-left">
          <span class="icon">⏱</span>
          <span class="title">시간순 블록 타임랩스</span>
          <span class="badge">{{ blockState.renderedBlocks }} / {{ blockState.totalEvents }} 블록</span>
        </div>

        <div class="header-right">
          <div class="status-indicator">
            <span class="live-dot" :class="{'pulsing': blockState.isPlaying || blockState.progress >= 0.99}"></span>
            <span class="status-text">
              {{ blockState.isPlaying ? '재생 중' : (blockState.progress >= 0.99 ? '실시간 연동' : '일시정지') }}
            </span>
          </div>
          <button class="close-btn" @click="toggleOpen" title="타임랩스 최소화">✕</button>
        </div>
      </div>

      <!-- Recent player placement alert if available -->
      <div v-if="blockState.latestPlayer" class="recent-player-bar">
        <span class="player-tag">최근 설치:</span>
        <span class="player-name">{{ blockState.latestPlayer }}</span>
        <span class="block-name">{{ blockState.latestBlock }}</span>
      </div>

      <!-- Scrubber Timeline Slider -->
      <div class="slider-row">
        <span class="time-label start" title="서버 시작">00:00</span>
        <div class="slider-wrapper">
          <input
              type="range"
              min="0"
              max="1"
              step="0.001"
              :value="blockState.progress"
              @input="onSeek"
              class="timeline-slider"
              title="시간순 스크롤 / 탐색"
          />
          <div class="slider-fill" :style="{width: (blockState.progress * 100) + '%'}"></div>
        </div>
        <span class="time-label current" title="현재 시점 / 총 시간">
          {{ formatTime(blockState.currentTime - blockState.serverStartTime) }}
        </span>
      </div>

      <!-- Control Buttons -->
      <div class="controls-row">
        <div class="playback-controls">
          <button class="ctrl-btn jump-btn" @click="jumpStart" title="맨 처음으로 (서버 시작)">⏮</button>
          <button class="ctrl-btn play-btn" @click="togglePlay" :title="blockState.isPlaying ? '일시정지' : '재생'">
            {{ blockState.isPlaying ? '⏸ 일시정지' : '▶ 재생' }}
          </button>
          <button class="ctrl-btn live-btn" :class="{'active': blockState.progress >= 0.99 && !blockState.isPlaying}" @click="jumpEnd" title="현재 실시간 시점으로 바로가기">
            <span class="live-dot-inline"></span> 실시간
          </button>
        </div>

        <!-- Speed options -->
        <div class="speed-group" title="재생 배속">
          <button
              v-for="s in [1, 2, 5, 10, 25]"
              :key="s"
              class="speed-btn"
              :class="{'active': blockState.speed === s}"
              @click="setSpeed(s)"
          >
            {{ s }}x
          </button>
        </div>

        <!-- Camera follow toggle -->
        <button
            class="ctrl-btn follow-btn"
            :class="{'active': blockState.autoFollow}"
            @click="toggleFollow"
            title="설치 위치 자동 시점 추적"
        >
          🎯 추적
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "TimelapseBar",
  computed: {
    appState() {
      return this.$bluemap ? this.$bluemap.appState : null;
    },
    blockManager() {
      return this.appState ? this.appState.blockManager : (this.$bluemap ? this.$bluemap.blockManager : null);
    },
    blockState() {
      return this.appState && this.appState.blockState ? this.appState.blockState : (this.blockManager ? this.blockManager.data : null);
    }
  },
  methods: {
    toggleOpen() {
      if (this.blockManager) {
        this.blockManager.toggleTimelapse();
      }
    },
    togglePlay() {
      if (this.blockManager) {
        this.blockManager.togglePlay();
      }
    },
    onSeek(e) {
      if (this.blockManager) {
        this.blockManager.seek(parseFloat(e.target.value));
      }
    },
    setSpeed(s) {
      if (this.blockManager) {
        this.blockManager.setSpeed(s);
      }
    },
    jumpStart() {
      if (this.blockManager) {
        this.blockManager.seek(0.0);
      }
    },
    jumpEnd() {
      if (this.blockManager) {
        this.blockManager.seek(1.0);
        this.blockManager.pause();
      }
    },
    toggleFollow() {
      if (this.blockState) {
        this.blockState.autoFollow = !this.blockState.autoFollow;
      }
    },
    formatTime(ms) {
      if (!ms || ms < 0) ms = 0;
      let totalSec = Math.floor(ms / 1000);
      let hours = Math.floor(totalSec / 3600);
      let minutes = Math.floor((totalSec % 3600) / 60);
      let seconds = totalSec % 60;

      if (hours > 0) {
        return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
      }
      return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }
  }
};
</script>

<style lang="scss" scoped>
.timelapse-container {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10001;
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  user-select: none;
  font-family: inherit;
  width: 92%;
  max-width: 560px;

  @media (max-width: 600px) {
    bottom: 16px;
    width: 95%;
  }

  .timelapse-toggle-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    background: rgba(22, 24, 32, 0.92);
    backdrop-filter: blur(10px);
    color: #f0f0f0;
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 28px;
    padding: 10px 20px;
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.45);
    transition: all 0.2s ease;

    &:hover {
      background: rgba(35, 38, 52, 0.98);
      border-color: #00e5ff;
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 229, 255, 0.25);
    }

    .icon {
      font-size: 1.15rem;
    }

    .badge {
      background: #00bcd4;
      color: #000;
      font-size: 0.75rem;
      font-weight: bold;
      padding: 2px 8px;
      border-radius: 12px;
    }

    .live-dot-mini {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #4caf50;

      &.pulsing {
        background: #00e5ff;
        box-shadow: 0 0 8px #00e5ff;
        animation: pulse 1.5s infinite;
      }
    }
  }

  .timelapse-panel {
    width: 100%;
    background: rgba(18, 20, 28, 0.94);
    backdrop-filter: blur(14px);
    border: 1px solid rgba(255, 255, 255, 0.16);
    border-radius: 16px;
    padding: 14px 18px;
    box-shadow: 0 10px 36px rgba(0, 0, 0, 0.6);
    display: flex;
    flex-direction: column;
    gap: 10px;

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        display: flex;
        align-items: center;
        gap: 8px;

        .icon {
          font-size: 1.1rem;
        }

        .title {
          font-weight: 700;
          font-size: 0.95rem;
          color: #fff;
        }

        .badge {
          background: rgba(0, 229, 255, 0.15);
          border: 1px solid rgba(0, 229, 255, 0.35);
          color: #00e5ff;
          font-size: 0.75rem;
          font-weight: 600;
          padding: 2px 8px;
          border-radius: 10px;
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 10px;

        .status-indicator {
          display: flex;
          align-items: center;
          gap: 6px;

          .live-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #4caf50;

            &.pulsing {
              background: #ff5252;
              box-shadow: 0 0 8px #ff5252;
              animation: pulse 1.5s infinite;
            }
          }

          .status-text {
            font-size: 0.8rem;
            font-weight: 600;
            color: #ccc;
          }
        }

        .close-btn {
          background: rgba(255, 255, 255, 0.1);
          border: none;
          color: #aaa;
          font-size: 0.85rem;
          border-radius: 50%;
          width: 24px;
          height: 24px;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          transition: all 0.15s ease;

          &:hover {
            background: rgba(255, 255, 255, 0.25);
            color: #fff;
          }
        }
      }
    }

    .recent-player-bar {
      font-size: 0.78rem;
      color: #aaa;
      display: flex;
      gap: 6px;
      align-items: center;
      background: rgba(255, 255, 255, 0.04);
      padding: 4px 10px;
      border-radius: 6px;

      .player-tag {
        color: #777;
      }
      .player-name {
        color: #ffd54f;
        font-weight: 600;
      }
      .block-name {
        color: #00e5ff;
        font-family: monospace;
      }
    }

    .slider-row {
      display: flex;
      align-items: center;
      gap: 10px;

      .time-label {
        font-size: 0.8rem;
        font-variant-numeric: tabular-nums;
        font-weight: 600;
        color: #888;
        min-width: 44px;

        &.current {
          color: #00e5ff;
          text-align: right;
        }
      }

      .slider-wrapper {
        position: relative;
        flex: 1;
        display: flex;
        align-items: center;

        .slider-fill {
          position: absolute;
          left: 0;
          height: 6px;
          background: linear-gradient(90deg, #0091ea, #00e5ff);
          border-radius: 3px;
          pointer-events: none;
          z-index: 1;
        }

        .timeline-slider {
          position: relative;
          z-index: 2;
          width: 100%;
          height: 6px;
          -webkit-appearance: none;
          appearance: none;
          background: rgba(255, 255, 255, 0.15);
          border-radius: 3px;
          outline: none;
          cursor: pointer;
          margin: 0;

          &::-webkit-slider-thumb {
            -webkit-appearance: none;
            appearance: none;
            width: 18px;
            height: 18px;
            border-radius: 50%;
            background: #ffffff;
            border: 2px solid #00e5ff;
            box-shadow: 0 0 10px rgba(0, 229, 255, 0.8);
            cursor: pointer;
            transition: transform 0.1s ease;

            &:hover {
              transform: scale(1.25);
            }
          }
        }
      }
    }

    .controls-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 8px;

      .playback-controls {
        display: flex;
        align-items: center;
        gap: 6px;
      }

      .ctrl-btn {
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.14);
        color: #eee;
        border-radius: 8px;
        padding: 7px 12px;
        font-size: 0.85rem;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 5px;
        transition: all 0.15s ease;

        &:hover {
          background: rgba(255, 255, 255, 0.18);
        }

        &.jump-btn {
          padding: 7px 10px;
          font-size: 0.95rem;
        }

        &.play-btn {
          background: #00bcd4;
          color: #000;
          font-weight: 700;
          padding: 7px 16px;

          &:hover {
            background: #26c6da;
          }
        }

        &.live-btn {
          background: rgba(244, 67, 54, 0.15);
          border-color: rgba(244, 67, 54, 0.4);
          color: #ff5252;

          .live-dot-inline {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #ff5252;
          }

          &.active {
            background: #ff5252;
            color: #fff;
            .live-dot-inline {
              background: #fff;
            }
          }

          &:hover {
            background: rgba(244, 67, 54, 0.3);
          }
        }

        &.follow-btn {
          font-size: 0.8rem;
          padding: 7px 10px;

          &.active {
            background: #ff9800;
            color: #000;
            font-weight: bold;
          }
        }
      }

      .speed-group {
        display: flex;
        gap: 3px;
        background: rgba(0, 0, 0, 0.35);
        padding: 3px;
        border-radius: 8px;

        .speed-btn {
          background: transparent;
          border: none;
          color: #888;
          font-size: 0.75rem;
          font-weight: 600;
          padding: 4px 7px;
          border-radius: 5px;
          cursor: pointer;

          &.active {
            background: rgba(255, 255, 255, 0.22);
            color: #fff;
          }

          &:hover:not(.active) {
            color: #ddd;
          }
        }
      }
    }
  }
}

@keyframes pulse {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(1.15); }
  100% { opacity: 1; transform: scale(1); }
}
</style>
