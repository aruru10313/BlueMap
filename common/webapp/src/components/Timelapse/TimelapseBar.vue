<template>
  <div v-if="blockManager && blockState" class="timelapse-container" :class="{'open': blockState.timelapseOpen}">
    <!-- Floating Collapsed Pill (when closed) -->
    <div v-if="!blockState.timelapseOpen" class="collapsed-container">
      <button class="timelapse-toggle-btn" @click="toggleOpen" title="타임랩스 / 실시간 타임라인 열기">
        <span class="icon">⏱</span>
        <span class="label">타임랩스</span>
        <span v-if="blockState.renderedBlocks > 0" class="badge">
          {{ blockState.renderedBlocks }}
        </span>
        <span class="live-dot-mini" :class="{'pulsing': blockState.isPlaying || blockState.progress >= 0.99}"></span>
      </button>

      <button class="quick-sync-btn" :class="{'syncing': blockState.isSyncing}" @click.stop="syncMap" title="맵 및 블록 즉시 동기화 (새로고침 없이 갱신)">
        <span class="sync-icon" :class="{'spinning': blockState.isSyncing}">🔄</span>
      </button>
    </div>

    <!-- Main Expanded Control Panel -->
    <!-- Main Expanded Control Panel -->
    <div v-else class="timelapse-panel">
      <!-- Row 1: Header + User Filter Chip + Status & Sync -->
      <div class="panel-header">
        <div class="header-left">
          <span class="icon">⏱</span>
          <span class="title">타임랩스</span>
          <span class="badge">{{ blockState.renderedBlocks }} / {{ blockState.totalEvents }}</span>
          <span v-if="blockState.syncStatusText" class="sync-status-badge">{{ blockState.syncStatusText }}</span>

          <!-- Inline compact user selector chip with custom flicker-free dropdown -->
          <div class="user-chip" :class="{'active': blockState.selectedPlayer, 'open': userDropdownOpen}" @click.stop="toggleUserDropdown" title="유저별 타임랩스 필터">
            <span class="chip-icon">👤</span>
            <span class="chip-label">{{ blockState.selectedPlayer || '전체 유저' }}</span>
            <span class="chip-arrow" :class="{'rotated': userDropdownOpen}">▾</span>
            <button v-if="blockState.selectedPlayer" class="chip-clear" @click.stop="clearPlayerFilter" title="전체 유저로">✕</button>

            <!-- Custom Dropdown Menu -->
            <div v-if="userDropdownOpen" class="user-dropdown-menu" @click.stop>
              <div
                  class="dropdown-item"
                  :class="{'selected': !blockState.selectedPlayer}"
                  @click="selectPlayer('')"
              >
                <span class="item-name">👤 전체 유저</span>
                <span v-if="!blockState.selectedPlayer" class="check">✓</span>
              </div>
              <div
                  v-for="player in blockState.playersList"
                  :key="player"
                  class="dropdown-item"
                  :class="{'selected': blockState.selectedPlayer === player}"
                  @click="selectPlayer(player)"
              >
                <span class="item-name">{{ player }}</span>
                <span v-if="blockState.selectedPlayer === player" class="check">✓</span>
              </div>
            </div>
          </div>

          <!-- Inline compact date selector chip -->
          <div class="date-chip" :class="{'active': blockState.dateFilterMode !== 'all', 'open': dateDropdownOpen}" @click.stop="toggleDateDropdown" title="날짜별 타임랩스 필터">
            <span class="chip-icon">📅</span>
            <span class="chip-label">{{ dateLabel }}</span>
            <span class="chip-arrow" :class="{'rotated': dateDropdownOpen}">▾</span>

            <!-- Custom Date Dropdown Menu -->
            <div v-if="dateDropdownOpen" class="date-dropdown-menu" @click.stop>
              <div
                  class="dropdown-item"
                  :class="{'selected': blockState.dateFilterMode === 'all'}"
                  @click="selectDateFilter('all')"
              >
                <span class="item-name">🌐 전체 기록</span>
                <span v-if="blockState.dateFilterMode === 'all'" class="check">✓</span>
              </div>
              <div
                  class="dropdown-item"
                  :class="{'selected': blockState.dateFilterMode === 'today'}"
                  @click="selectDateFilter('today')"
              >
                <span class="item-name">☀️ 오늘</span>
                <span v-if="blockState.dateFilterMode === 'today'" class="check">✓</span>
              </div>
              <div
                  class="dropdown-item"
                  :class="{'selected': blockState.dateFilterMode === 'yesterday'}"
                  @click="selectDateFilter('yesterday')"
              >
                <span class="item-name">🌙 어제</span>
                <span v-if="blockState.dateFilterMode === 'yesterday'" class="check">✓</span>
              </div>
              <div v-if="blockState.availableDates && blockState.availableDates.length > 0" class="dropdown-divider"></div>
              <div
                  v-for="d in (blockState.availableDates || [])"
                  :key="d"
                  class="dropdown-item"
                  :class="{'selected': blockState.dateFilterMode === 'date' && blockState.selectedDate === d}"
                  @click="selectDateFilter('date', d)"
              >
                <span class="item-name">📅 {{ formatDateLabel(d) }}</span>
                <span v-if="blockState.dateFilterMode === 'date' && blockState.selectedDate === d" class="check">✓</span>
              </div>
            </div>
          </div>
        </div>

        <div class="header-right">
          <button class="sync-btn" :class="{'syncing': blockState.isSyncing}" @click="syncMap" title="맵 및 블록 즉시 동기화">
            <span class="sync-icon" :class="{'spinning': blockState.isSyncing}">🔄</span>
          </button>

          <button class="live-pill-btn" :class="{'active': blockState.progress >= 0.99 && !blockState.isPlaying}" @click="jumpEnd" title="실시간 시점으로 이동">
            <span class="live-dot" :class="{'pulsing': blockState.isPlaying || blockState.progress >= 0.99}"></span>
            <span class="live-text">{{ blockState.isPlaying ? '재생 중' : (blockState.progress >= 0.99 ? '실시간' : '일시정지') }}</span>
          </button>

          <button class="close-btn" @click="toggleOpen" title="최소화">✕</button>
        </div>
      </div>

      <!-- Row 2: Scrubber Timeline Slider -->
      <div class="slider-row">
        <span class="time-label start" title="시작 시점">{{ startTimeLabel }}</span>
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
        <span class="time-label current" title="현재 시점">
          {{ currentTimeLabel }}
        </span>
      </div>

      <!-- Row 3: Playback Controls & Speed Segmented Control -->
      <div class="controls-row">
        <div class="main-action-group">
          <button class="ctrl-btn jump-btn" @click="jumpStart" title="처음으로">⏮</button>
          <button class="ctrl-btn play-btn" :class="{'playing': blockState.isPlaying}" @click="togglePlay" :title="blockState.isPlaying ? '일시정지' : '재생'">
            <span class="play-icon">{{ blockState.isPlaying ? '⏸' : '▶' }}</span>
            <span class="play-label">{{ blockState.isPlaying ? '정지' : '재생' }}</span>
          </button>
          <button
              class="ctrl-btn follow-btn"
              :class="{'active': blockState.autoFollow}"
              @click="toggleFollow"
              title="설치 위치 자동 시점 추적"
          >
            🎯 추적
          </button>
          <button
              class="ctrl-btn highlight-btn"
              :class="{'active': blockState.highlightOnly}"
              @click="toggleHighlight"
              :title="blockState.highlightOnly ? '2초 강조 모드: 블록이 2초간 보이고 사라집니다' : '누적 모드: 설치된 블록이 사라지지 않고 유지됩니다'"
          >
            {{ blockState.highlightOnly ? '✨ 2초 강조' : '🏗 누적' }}
          </button>
        </div>

        <!-- Speed Segmented Control -->
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
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "TimelapseBar",
  data() {
    return {
      userDropdownOpen: false,
      dateDropdownOpen: false
    };
  },
  mounted() {
    document.addEventListener("click", this.handleDocumentClick);
  },
  beforeUnmount() {
    document.removeEventListener("click", this.handleDocumentClick);
  },
  computed: {
    appState() {
      return this.$bluemap ? this.$bluemap.appState : null;
    },
    blockManager() {
      return this.appState ? this.appState.blockManager : (this.$bluemap ? this.$bluemap.blockManager : null);
    },
    blockState() {
      return this.appState && this.appState.blockState ? this.appState.blockState : (this.blockManager ? this.blockManager.data : null);
    },
    dateLabel() {
      if (!this.blockState) return "전체 기록";
      if (this.blockState.dateFilterMode === "today") return "오늘";
      if (this.blockState.dateFilterMode === "yesterday") return "어제";
      if (this.blockState.dateFilterMode === "date" && this.blockState.selectedDate) {
        return this.formatDateLabel(this.blockState.selectedDate);
      }
      return "전체 기록";
    },
    startTimeLabel() {
      return "00:00";
    },
    currentTimeLabel() {
      if (!this.blockState) return "00:00";
      if (this.blockState.dateFilterMode === "today" || this.blockState.dateFilterMode === "yesterday" || this.blockState.dateFilterMode === "date") {
        return this.formatClock(this.blockState.currentTime);
      }
      return this.formatTime(this.blockState.currentTime - this.blockState.serverStartTime);
    }
  },
  methods: {
    handleDocumentClick(e) {
      if (this.userDropdownOpen && !this.$el?.querySelector(".user-chip")?.contains(e.target)) {
        this.userDropdownOpen = false;
      }
      if (this.dateDropdownOpen && !this.$el?.querySelector(".date-chip")?.contains(e.target)) {
        this.dateDropdownOpen = false;
      }
    },
    toggleUserDropdown() {
      this.userDropdownOpen = !this.userDropdownOpen;
    },
    selectPlayer(player) {
      if (this.blockManager) {
        this.blockManager.setPlayerFilter(player);
      }
      this.userDropdownOpen = false;
    },
    toggleDateDropdown() {
      this.dateDropdownOpen = !this.dateDropdownOpen;
    },
    selectDateFilter(mode, dateStr = "") {
      if (this.blockManager) {
        this.blockManager.setDateFilter(mode, dateStr);
      }
      this.dateDropdownOpen = false;
    },
    toggleHighlight() {
      if (this.blockManager) {
        this.blockManager.toggleHighlightOnly();
      }
    },
    formatDateLabel(dateStr) {
      if (!dateStr) return "";
      let parts = dateStr.split("-");
      if (parts.length === 3) {
        return `${Number(parts[1])}월 ${Number(parts[2])}일`;
      }
      return dateStr;
    },
    formatClock(ts) {
      if (!ts) return "00:00:00";
      let d = new Date(ts);
      let h = String(d.getHours()).padStart(2, "0");
      let m = String(d.getMinutes()).padStart(2, "0");
      let s = String(d.getSeconds()).padStart(2, "0");
      return `${h}:${m}:${s}`;
    },
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
    syncMap() {
      if (this.blockManager) {
        this.blockManager.syncMap();
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
    onPlayerChange(e) {
      if (this.blockManager) {
        this.blockManager.setPlayerFilter(e.target.value);
      }
    },
    clearPlayerFilter() {
      if (this.blockManager) {
        this.blockManager.setPlayerFilter("");
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
  bottom: calc(14px + env(safe-area-inset-bottom, 0px));
  left: 50%;
  transform: translateX(-50%);
  z-index: 10001;
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  user-select: none;
  font-family: inherit;
  width: calc(100% - 24px);
  max-width: 540px;
  touch-action: manipulation;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  .collapsed-container {
    display: flex;
    align-items: center;
    gap: 8px;

    .timelapse-toggle-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      background: rgba(16, 20, 30, 0.92);
      backdrop-filter: blur(14px);
      -webkit-backdrop-filter: blur(14px);
      color: #f0f0f0;
      border: 1px solid rgba(255, 255, 255, 0.22);
      border-radius: 28px;
      padding: 10px 18px;
      font-size: 0.92rem;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 6px 22px rgba(0, 0, 0, 0.5);
      touch-action: manipulation;
      transition: all 0.2s ease;

      &:hover, &:active {
        background: rgba(28, 34, 48, 0.98);
        border-color: #00e5ff;
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(0, 229, 255, 0.25);
      }

      .icon {
        font-size: 1.1rem;
      }

      .badge {
        background: #00e5ff;
        color: #000;
        font-size: 0.75rem;
        font-weight: 700;
        padding: 2px 7px;
        border-radius: 10px;
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

    .quick-sync-btn {
      background: rgba(16, 20, 30, 0.92);
      backdrop-filter: blur(14px);
      -webkit-backdrop-filter: blur(14px);
      color: #00e5ff;
      border: 1px solid rgba(0, 229, 255, 0.35);
      border-radius: 50%;
      width: 42px;
      height: 42px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.15rem;
      cursor: pointer;
      box-shadow: 0 6px 22px rgba(0, 0, 0, 0.5);
      touch-action: manipulation;
      transition: all 0.2s ease;

      &:hover, &:active {
        background: rgba(0, 229, 255, 0.2);
        border-color: #00e5ff;
        transform: scale(1.06);
      }

      &.syncing {
        border-color: #ffb74d;
        color: #ffb74d;
      }
    }
  }

  .timelapse-panel {
    width: 100%;
    box-sizing: border-box;
    background: rgba(14, 18, 28, 0.94);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.16);
    border-radius: 18px;
    padding: 12px 14px;
    box-shadow: 0 10px 36px rgba(0, 0, 0, 0.65);
    display: flex;
    flex-direction: column;
    gap: 8px;

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        display: flex;
        align-items: center;
        gap: 6px;

        .icon {
          font-size: 1.05rem;
        }

        .title {
          font-weight: 700;
          font-size: 0.92rem;
          color: #fff;
        }

        .badge {
          background: rgba(0, 229, 255, 0.15);
          border: 1px solid rgba(0, 229, 255, 0.35);
          color: #00e5ff;
          font-size: 0.72rem;
          font-weight: 700;
          padding: 2px 7px;
          border-radius: 8px;
        }

        .sync-status-badge {
          background: rgba(76, 175, 80, 0.2);
          border: 1px solid rgba(76, 175, 80, 0.5);
          color: #81c784;
          font-size: 0.72rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 6px;
        }

        .user-chip {
          position: relative;
          display: flex;
          align-items: center;
          gap: 4px;
          background: rgba(255, 255, 255, 0.08);
          border: 1px solid rgba(255, 255, 255, 0.18);
          border-radius: 14px;
          padding: 3px 8px;
          margin-left: 4px;
          cursor: pointer;
          transition: all 0.15s ease;
          user-select: none;

          &:hover, &.open {
            background: rgba(255, 255, 255, 0.14);
            border-color: rgba(255, 213, 79, 0.5);
          }

          &.active {
            border-color: #ffd54f;
            background: rgba(255, 213, 79, 0.14);
          }

          .chip-icon {
            font-size: 0.75rem;
          }

          .chip-label {
            color: #ffd54f;
            font-size: 0.74rem;
            font-weight: 600;
            max-width: 90px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .chip-arrow {
            font-size: 0.65rem;
            color: rgba(255, 255, 255, 0.6);
            transition: transform 0.15s ease;
            &.rotated {
              transform: rotate(180deg);
            }
          }

          .chip-clear {
            background: rgba(255, 255, 255, 0.15);
            border: none;
            color: #f87171;
            border-radius: 50%;
            width: 14px;
            height: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 0.65rem;
            cursor: pointer;
            padding: 0;
            margin-left: 2px;

            &:hover {
              background: rgba(248, 113, 113, 0.35);
              color: #ff4d4f;
            }
          }

          .user-dropdown-menu {
            position: absolute;
            bottom: calc(100% + 8px);
            left: 0;
            min-width: 135px;
            max-width: 200px;
            max-height: 180px;
            overflow-y: auto;
            background: rgba(18, 22, 34, 0.96);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.2);
            border-radius: 12px;
            padding: 4px;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.75);
            z-index: 10010;
            display: flex;
            flex-direction: column;
            gap: 2px;

            .dropdown-item {
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 6px 10px;
              border-radius: 8px;
              font-size: 0.76rem;
              font-weight: 500;
              color: #e2e8f0;
              cursor: pointer;
              transition: background 0.15s ease;

              &:hover {
                background: rgba(255, 255, 255, 0.1);
                color: #fff;
              }

              &.selected {
                color: #ffd54f;
                font-weight: 700;
                background: rgba(255, 213, 79, 0.15);
              }

              .item-name {
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
              }

              .check {
                font-size: 0.7rem;
                margin-left: 6px;
                color: #ffd54f;
              }
            }
          }
        }

        .date-chip {
          position: relative;
          display: flex;
          align-items: center;
          gap: 4px;
          background: rgba(255, 255, 255, 0.08);
          border: 1px solid rgba(255, 255, 255, 0.18);
          border-radius: 14px;
          padding: 3px 8px;
          margin-left: 2px;
          cursor: pointer;
          transition: all 0.15s ease;
          user-select: none;

          &:hover, &.open {
            background: rgba(255, 255, 255, 0.14);
            border-color: rgba(0, 229, 255, 0.5);
          }

          &.active {
            border-color: #00e5ff;
            background: rgba(0, 229, 255, 0.14);
          }

          .chip-icon {
            font-size: 0.75rem;
          }

          .chip-label {
            color: #00e5ff;
            font-size: 0.74rem;
            font-weight: 600;
            max-width: 90px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .chip-arrow {
            font-size: 0.65rem;
            color: rgba(255, 255, 255, 0.6);
            transition: transform 0.15s ease;
            &.rotated {
              transform: rotate(180deg);
            }
          }

          .date-dropdown-menu {
            position: absolute;
            bottom: calc(100% + 8px);
            left: 0;
            min-width: 140px;
            max-width: 200px;
            max-height: 200px;
            overflow-y: auto;
            background: rgba(18, 22, 34, 0.96);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.2);
            border-radius: 12px;
            padding: 4px;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.75);
            z-index: 10010;
            display: flex;
            flex-direction: column;
            gap: 2px;

            .dropdown-divider {
              height: 1px;
              background: rgba(255, 255, 255, 0.12);
              margin: 3px 0;
            }

            .dropdown-item {
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 6px 10px;
              border-radius: 8px;
              font-size: 0.76rem;
              font-weight: 500;
              color: #e2e8f0;
              cursor: pointer;
              transition: background 0.15s ease;

              &:hover {
                background: rgba(255, 255, 255, 0.1);
                color: #fff;
              }

              &.selected {
                color: #00e5ff;
                font-weight: 700;
                background: rgba(0, 229, 255, 0.15);
              }

              .item-name {
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
              }

              .check {
                font-size: 0.7rem;
                margin-left: 6px;
                color: #00e5ff;
              }
            }
          }
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 8px;

        .sync-btn {
          background: rgba(0, 229, 255, 0.12);
          border: 1px solid rgba(0, 229, 255, 0.3);
          color: #00e5ff;
          width: 26px;
          height: 26px;
          border-radius: 50%;
          font-size: 0.82rem;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          touch-action: manipulation;
          transition: all 0.15s ease;

          &:hover, &:active {
            background: rgba(0, 229, 255, 0.25);
            border-color: #00e5ff;
          }

          &.syncing {
            color: #ffb74d;
            border-color: #ffb74d;
            background: rgba(255, 183, 77, 0.15);
          }
        }

        .live-pill-btn {
          display: flex;
          align-items: center;
          gap: 5px;
          background: rgba(255, 255, 255, 0.06);
          border: 1px solid rgba(255, 255, 255, 0.12);
          border-radius: 12px;
          padding: 3px 8px;
          cursor: pointer;
          transition: all 0.15s ease;

          .live-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #4ade80;

            &.pulsing {
              background: #00e5ff;
              box-shadow: 0 0 8px #00e5ff;
              animation: pulse 1.5s infinite;
            }
          }

          .live-text {
            font-size: 0.72rem;
            font-weight: 600;
            color: #94a3b8;
          }

          &.active {
            background: rgba(0, 229, 255, 0.15);
            border-color: rgba(0, 229, 255, 0.4);

            .live-text {
              color: #00e5ff;
            }
          }

          &:hover {
            background: rgba(255, 255, 255, 0.12);
          }
        }

        .close-btn {
          background: rgba(255, 255, 255, 0.1);
          border: none;
          color: #aaa;
          font-size: 0.82rem;
          border-radius: 50%;
          width: 24px;
          height: 24px;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          touch-action: manipulation;
          transition: all 0.15s ease;

          &:hover, &:active {
            background: rgba(255, 255, 255, 0.25);
            color: #fff;
          }
        }
      }
    }

    .slider-row {
      display: flex;
      align-items: center;
      gap: 8px;

      .time-label {
        font-size: 0.78rem;
        font-variant-numeric: tabular-nums;
        font-weight: 600;
        color: #888;
        min-width: 40px;

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
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: #ffffff;
            border: 2px solid #00e5ff;
            box-shadow: 0 0 10px rgba(0, 229, 255, 0.8);
            cursor: pointer;
            transition: transform 0.1s ease;

            &:active {
              transform: scale(1.3);
            }
          }
        }
      }
    }

    .controls-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 6px;

      .main-action-group {
        display: flex;
        align-items: center;
        gap: 5px;
      }

      .ctrl-btn {
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.14);
        color: #eee;
        border-radius: 8px;
        padding: 6px 11px;
        font-size: 0.82rem;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        touch-action: manipulation;
        transition: all 0.15s ease;

        &:hover, &:active {
          background: rgba(255, 255, 255, 0.18);
        }

        &.jump-btn {
          padding: 6px 9px;
          font-size: 0.9rem;
        }

        &.play-btn {
          background: linear-gradient(135deg, #0284c7, #2563eb);
          border: 1px solid rgba(56, 189, 248, 0.4);
          color: #fff;
          font-weight: 700;
          padding: 6px 14px;
          box-shadow: 0 2px 10px rgba(37, 99, 235, 0.35);

          &:hover, &:active {
            background: linear-gradient(135deg, #0369a1, #1d4ed8);
            transform: translateY(-1px);
          }

          &.playing {
            background: linear-gradient(135deg, #e11d48, #be123c);
            border-color: rgba(244, 63, 94, 0.4);
            box-shadow: 0 2px 10px rgba(225, 29, 72, 0.35);
          }
        }

        &.follow-btn {
          font-size: 0.78rem;
          padding: 6px 9px;

          &.active {
            background: #ff9800;
            color: #000;
            font-weight: 700;
          }
        }

        &.highlight-btn {
          font-size: 0.76rem;
          padding: 6px 9px;

          &.active {
            background: rgba(0, 229, 255, 0.2);
            border-color: #00e5ff;
            color: #00e5ff;
            font-weight: 700;
            box-shadow: 0 0 10px rgba(0, 229, 255, 0.25);
          }
        }
      }

      .speed-group {
        display: flex;
        gap: 2px;
        background: rgba(0, 0, 0, 0.35);
        padding: 2px;
        border-radius: 7px;

        .speed-btn {
          background: transparent;
          border: none;
          color: #888;
          font-size: 0.72rem;
          font-weight: 600;
          padding: 4px 6px;
          border-radius: 5px;
          cursor: pointer;
          touch-action: manipulation;

          &.active {
            background: rgba(255, 255, 255, 0.22);
            color: #fff;
          }

          &:hover:not(.active), &:active:not(.active) {
            color: #ddd;
          }
        }
      }

      /* Mobile layout adjustments */
      @media (max-width: 520px) {
        flex-direction: column;
        align-items: stretch;
        gap: 7px;

        .main-action-group {
          display: flex;
          justify-content: space-between;
          gap: 5px;
          width: 100%;

          .ctrl-btn {
            flex: 1;
            padding: 8px 3px;
            font-size: 0.8rem;
            min-height: 38px;
          }

          .play-btn {
            flex: 1.3;
          }
        }

        .speed-group {
          display: flex;
          justify-content: space-between;
          width: 100%;

          .speed-btn {
            flex: 1;
            text-align: center;
            padding: 6px 2px;
            font-size: 0.76rem;
            min-height: 30px;
          }
        }
      }
    }
  }
}

@keyframes pulse {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(1.2); }
  100% { opacity: 1; transform: scale(1); }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.sync-icon.spinning {
  display: inline-block;
  animation: spin 0.7s linear infinite;
}
</style>
