<template>
  <div v-if="blockManager && blockState" class="timelapse-container" :class="{'open': blockState.timelapseOpen}">
    <!-- Toggle Button (Always accessible, mobile touch friendly) -->
    <button class="timelapse-toggle-btn" @click="toggleOpen" :title="blockState.timelapseOpen ? '타임랩스 닫기' : '타임랩스 / 실시간 블록 열기'">
      <span class="icon">⏱</span>
      <span class="label">{{ blockState.timelapseOpen ? '타임랩스 닫기' : '실시간 / 타임랩스' }}</span>
      <span v-if="!blockState.timelapseOpen && blockState.renderedBlocks > 0" class="badge">
        {{ blockState.renderedBlocks }}
      </span>
    </button>

    <!-- Main Expanded Control Panel -->
    <div v-if="blockState.timelapseOpen" class="timelapse-panel">
      <!-- Top info bar -->
      <div class="panel-header">
        <div class="status-indicator">
          <span class="live-dot" :class="{'pulsing': blockState.isPlaying || blockState.progress >= 0.99}"></span>
          <span class="status-text">
            {{ blockState.isPlaying ? '타임랩스 재생 중' : (blockState.progress >= 0.99 ? '실시간 연동 중' : '일시정지') }}
          </span>
        </div>

        <div class="block-info">
          <span class="highlight">{{ blockState.renderedBlocks }}</span> / {{ blockState.totalEvents }} 블록
          <span v-if="blockState.latestPlayer" class="recent-player">
            • <b>{{ blockState.latestPlayer }}</b>: {{ blockState.latestBlock }}
          </span>
        </div>
      </div>

      <!-- Scrubber Timeline Slider -->
      <div class="slider-row">
        <span class="time-label">{{ formatTime(blockState.currentTime - blockState.serverStartTime) }}</span>
        <input
            type="range"
            min="0"
            max="1"
            step="0.001"
            :value="blockState.progress"
            @input="onSeek"
            class="timeline-slider"
        />
        <span class="time-label total">{{ formatTime(blockState.serverEndTime - blockState.serverStartTime) }}</span>
      </div>

      <!-- Control Buttons -->
      <div class="controls-row">
        <button class="ctrl-btn" @click="jumpStart" title="처음으로 (서버 시작)">⏮</button>
        <button class="ctrl-btn play-btn" @click="togglePlay" :title="blockState.isPlaying ? '일시정지' : '재생'">
          {{ blockState.isPlaying ? '⏸' : '▶' }}
        </button>
        <button class="ctrl-btn" @click="jumpEnd" title="현재 실시간으로">⏭</button>

        <!-- Speed options -->
        <div class="speed-group">
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
  data() {
    return {
      bluemap: this.$bluemap
    };
  },
  computed: {
    blockManager() {
      return this.bluemap ? this.bluemap.blockManager : null;
    },
    blockState() {
      return this.blockManager ? this.blockManager.data : null;
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
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10001;
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  user-select: none;
  font-family: inherit;

  @media (max-width: 600px) {
    bottom: 95px;
    width: 94%;
  }

  .timelapse-toggle-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    background: rgba(26, 26, 32, 0.88);
    backdrop-filter: blur(8px);
    color: #e0e0e0;
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 24px;
    padding: 7px 16px;
    font-size: 0.9rem;
    font-weight: 500;
    cursor: pointer;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
    transition: all 0.2s ease;

    &:hover {
      background: rgba(40, 40, 50, 0.95);
      border-color: rgba(255, 255, 255, 0.3);
      transform: translateY(-1px);
    }

    .icon {
      font-size: 1.1rem;
    }

    .badge {
      background: #00bcd4;
      color: #000;
      font-size: 0.75rem;
      font-weight: bold;
      padding: 1px 7px;
      border-radius: 10px;
    }
  }

  .timelapse-panel {
    margin-top: 8px;
    width: 440px;
    max-width: 95vw;
    background: rgba(20, 20, 26, 0.92);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 14px;
    padding: 12px 16px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    gap: 10px;

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.85rem;
      color: #aaa;

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
          font-weight: 600;
          color: #ddd;
        }
      }

      .block-info {
        font-size: 0.8rem;
        .highlight {
          color: #00e5ff;
          font-weight: bold;
        }
        .recent-player {
          margin-left: 6px;
          color: #ccc;
          b {
            color: #ffd54f;
          }
        }
      }
    }

    .slider-row {
      display: flex;
      align-items: center;
      gap: 10px;

      .time-label {
        font-size: 0.75rem;
        font-variant-numeric: tabular-nums;
        color: #888;
        min-width: 44px;

        &.total {
          text-align: right;
        }
      }

      .timeline-slider {
        flex: 1;
        height: 6px;
        -webkit-appearance: none;
        appearance: none;
        background: rgba(255, 255, 255, 0.15);
        border-radius: 3px;
        outline: none;
        cursor: pointer;

        &::-webkit-slider-thumb {
          -webkit-appearance: none;
          appearance: none;
          width: 16px;
          height: 16px;
          border-radius: 50%;
          background: #00e5ff;
          box-shadow: 0 0 6px rgba(0, 229, 255, 0.6);
          cursor: pointer;
          transition: transform 0.1s ease;

          &:hover {
            transform: scale(1.2);
          }
        }
      }
    }

    .controls-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 6px;

      .ctrl-btn {
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.1);
        color: #eee;
        border-radius: 8px;
        padding: 5px 10px;
        font-size: 0.9rem;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
          background: rgba(255, 255, 255, 0.18);
        }

        &.play-btn {
          background: #00bcd4;
          color: #000;
          font-weight: bold;
          padding: 6px 14px;
          font-size: 1rem;

          &:hover {
            background: #26c6da;
          }
        }

        &.follow-btn {
          font-size: 0.75rem;
          padding: 5px 8px;

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
        background: rgba(0, 0, 0, 0.3);
        padding: 2px;
        border-radius: 6px;

        .speed-btn {
          background: transparent;
          border: none;
          color: #888;
          font-size: 0.75rem;
          font-weight: 600;
          padding: 4px 6px;
          border-radius: 4px;
          cursor: pointer;

          &.active {
            background: rgba(255, 255, 255, 0.2);
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
  50% { opacity: 0.4; transform: scale(1.1); }
  100% { opacity: 1; transform: scale(1); }
}
</style>
